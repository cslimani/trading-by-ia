# train_dynamic_breakout_classifier_xgb_cv_compat.py
import warnings, os, random
warnings.filterwarnings("ignore", message=".*Falling back to prediction using DMatrix.*")

from datetime import datetime
import numpy as np
import pandas as pd
from joblib import dump
from itertools import product
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.metrics import roc_auc_score, average_precision_score, classification_report, confusion_matrix
from sklearn.isotonic import IsotonicRegression
import xgboost as xgb
from packaging import version

# -------- CONFIG --------
CSV_PATH = "/data/trading_ml/bars_after_break.csv"
TIME_COL, GROUP_COL, TARGET_COL, T_SINCE_COL = "timestamp", "accum_id", "y", "f_time_since_break"
TEST_GROUP_RATIO = 0.2
N_SPLITS = 5
RANDOM_SEED = 42
os.makedirs("data", exist_ok=True)
MODEL_OUT = "data/breakout_model_dynamic_gpu_cv.joblib"
BOOSTER_JSON_OUT = "data/breakout_model_dynamic_gpu_cv.json"
PREDICTIONS_OUT = "data/oos_predictions_dynamic_gpu_cv.csv"

N_TRIALS = 12                 # nb de combinaisons testées via xgb.cv
NUM_BOOST_ROUND = 3000
EARLY_STOPPING_ROUNDS = 150

# -------- UTILS --------
def load_df(path):
    df = pd.read_csv(path, parse_dates=[TIME_COL])
    df = df.replace([np.inf, -np.inf], np.nan)
    return df.sort_values([TIME_COL, GROUP_COL, T_SINCE_COL]).reset_index(drop=True)

def check_columns(df, feats):
    required = {GROUP_COL, TIME_COL, TARGET_COL, T_SINCE_COL}
    miss = required - set(df.columns)
    if miss: raise ValueError(f"Colonnes manquantes: {miss}")
    for f in feats:
        if f not in df.columns:
            raise ValueError(f"Feature manquante: {f}")

def make_group_order(df):
    return list(df.groupby(GROUP_COL)[TIME_COL].min().sort_values().index)

def split_holdout_by_group(df, ratio=0.2):
    grp_order = make_group_order(df)
    n_test = max(1, int(len(grp_order)*ratio))
    test_groups = set(grp_order[-n_test:])
    mask_test = df[GROUP_COL].isin(test_groups)
    return (~mask_test).to_numpy(), mask_test.to_numpy()

def make_group_time_cv_splits(df, n_splits=5):
    grp_order = make_group_order(df)
    n_splits = min(n_splits, len(grp_order))
    sizes = np.full(n_splits, len(grp_order)//n_splits, dtype=int)
    sizes[: len(grp_order)%n_splits] += 1
    bounds = np.cumsum(sizes)
    splits=[]
    for i in range(1, n_splits):
        tr_g = set(grp_order[:bounds[i-1]])
        va_g = set(grp_order[bounds[i-1]:bounds[i]])
        tr = df.index[df[GROUP_COL].isin(tr_g)].to_numpy()
        va = df.index[df[GROUP_COL].isin(va_g)].to_numpy()
        splits.append((tr, va))
    return splits

def audit_splits(df, splits):
    ok=True
    for k,(tr,va) in enumerate(splits,1):
        gtr=set(df.iloc[tr][GROUP_COL]); gva=set(df.iloc[va][GROUP_COL])
        if gtr & gva: print(f"[Fold {k}] Ids partagés: {gtr & gva}"); ok=False
        if df.iloc[tr][TIME_COL].max() >= df.iloc[va][TIME_COL].min():
            print(f"[Fold {k}] Train déborde sur Val."); ok=False
    return ok

def filter_cv_splits_with_both_classes(cv_splits, y_all):
    safe=[]
    for tr,va in cv_splits:
        if np.unique(y_all[va]).size==2: safe.append((tr,va))
    if not safe: raise ValueError("Aucun pli de CV n'a les deux classes.")
    return safe

def make_weights(df, y):
    seq_len = df.groupby(GROUP_COL).size()
    w_group = df[GROUP_COL].map(1.0/seq_len)
    p = y.mean()
    w_pos = 0.5/p if p>0 else 1.0
    w_neg = 0.5/(1-p) if p<1 else 1.0
    w_class = np.where(y==1, w_pos, w_neg)
    return (w_group.values * w_class).astype(np.float32)

def choose_param_trials(rng):
    grid = {
        "max_depth":[3,5,7],
        "learning_rate":[0.03,0.05,0.1],
        "min_child_weight":[1,5,10],
        "subsample":[0.7,0.85,1.0],
        "colsample_bytree":[0.7,0.9,1.0],
        "reg_lambda":[1.0,5.0,10.0],
        "reg_alpha":[0.0,0.1,1.0],
    }
    all_combos=list(product(*grid.values()))
    rng.shuffle(all_combos)
    keys=list(grid.keys())
    return [dict(zip(keys, vals)) for vals in all_combos[:N_TRIALS]]

def gpu_params(monotonic=None):
    v = version.parse(xgb.__version__)
    # XGBoost 2.x : device='cuda' + tree_method='hist'
    # XGBoost 1.7.x : tree_method='gpu_hist', predictor='gpu_predictor'
    params = {
        "objective":"binary:logistic",
        "eval_metric":"auc",
        "verbosity":1,
        "nthread":1
    }
    if v >= version.parse("2.0.0"):
        params.update({"device":"cuda","tree_method":"hist"})
    else:
        params.update({"tree_method":"gpu_hist","predictor":"gpu_predictor"})
    # if monotonic is not None:
    #     params["monotone_constraints"] = "(" + ","join(map(str, monotonic)) + ")"
    return params

# -------- MAIN --------
def main():
    start = datetime.now()
    rng = random.Random(RANDOM_SEED)
    np.random.seed(RANDOM_SEED)

    df = load_df(CSV_PATH)
    # FEATURES = ["risk_ratio","accum_size","range_swing_high_ratio","t_since_break","swing_high_distance"]
    FEATURES = [c for c in df.columns if c.startswith("f_")]
    check_columns(df, FEATURES)
    X = df[FEATURES]; y = df[TARGET_COL].astype(int).values

    mask_tr, mask_te = split_holdout_by_group(df, TEST_GROUP_RATIO)
    X_train, y_train = X.loc[mask_tr], y[mask_tr]
    X_test,  y_test  = X.loc[mask_te], y[mask_te]
    df_train, df_test = df.loc[mask_tr], df.loc[mask_te]

    cv_raw = make_group_time_cv_splits(df_train, n_splits=N_SPLITS)
    cv_splits = filter_cv_splits_with_both_classes(cv_raw, y_train)
    assert audit_splits(df_train, cv_splits), "Problème dans les splits de CV."
    folds = [(tr.tolist(), va.tolist()) for tr, va in cv_splits]

    # --- prétraitement une fois ---
    preproc = ColumnTransformer([("num", SimpleImputer(strategy="median"), FEATURES)])
    Xtr_pre = preproc.fit_transform(X_train)
    if hasattr(Xtr_pre, "toarray"): Xtr_pre = Xtr_pre.toarray()
    Xtr_pre = Xtr_pre.astype(np.float32, copy=False)
    w_train = make_weights(df_train, y_train)

    # --- DMatrix unique pour la CV / entraînement (compatible cv) ---
    dtrain = xgb.DMatrix(Xtr_pre, label=y_train, weight=w_train)

    # (optionnel) contraintes monotones
    monotonic = None  # ex: [-1,0,0,0,0]
    base = gpu_params(monotonic)

    # --- mini HPO via xgb.cv ---
    trials = choose_param_trials(rng)
    best_auc, best_params, best_rounds = -np.inf, None, None
    cv_history = []

    for i, hp in enumerate(trials, 1):
        params = {**base, **hp}
        print(f"\n[CV {i}/{len(trials)}] {hp}")
        cvres = xgb.cv(
            params=params,
            dtrain=dtrain,
            num_boost_round=NUM_BOOST_ROUND,
            folds=folds,
            early_stopping_rounds=EARLY_STOPPING_ROUNDS,
            seed=RANDOM_SEED,
            shuffle=False,
            verbose_eval=False
        )
        best_idx = int(np.argmax(cvres["test-auc-mean"].values))
        auc_mean = float(cvres["test-auc-mean"].iloc[best_idx])
        rounds = best_idx + 1
        cv_history.append({"params": hp, "test_auc": auc_mean, "rounds": rounds})
        print(f"  -> test AUC={auc_mean:.5f} @ rounds={rounds}")
        if auc_mean > best_auc:
            best_auc, best_params, best_rounds = auc_mean, hp, rounds

    print("\nMeilleurs hyperparamètres:", best_params)
    print(f"Meilleur test AUC (CV): {best_auc:.5f} @ rounds={best_rounds}")

    # --- entraînement final ---
    final_params = {**base, **best_params}
    booster = xgb.train(params=final_params, dtrain=dtrain, num_boost_round=best_rounds)

    # --- calibration isotone ---
    dtrain_pred = xgb.DMatrix(Xtr_pre)
    proba_tr = booster.predict(dtrain_pred)
    iso = IsotonicRegression(out_of_bounds="clip").fit(proba_tr, y_train)

    # --- évaluation OOS ---
    Xte_pre = preproc.transform(X_test)
    if hasattr(Xte_pre, "toarray"): Xte_pre = Xte_pre.toarray()
    Xte_pre = Xte_pre.astype(np.float32, copy=False)
    dtest = xgb.DMatrix(Xte_pre)
    proba = booster.predict(dtest)
    proba_cal = iso.transform(proba)
    y_pred = (proba_cal >= 0.5).astype(int)

    print("\n=== OOS (toutes lignes) ===")
    print(f"Instances de test : {len(y_test)}")
    print("ROC AUC :", roc_auc_score(y_test, proba_cal))
    print("PR (Precision)  AUC :", average_precision_score(y_test, proba_cal))
    print("\nConfusion matrix (thr=0.5):\n", confusion_matrix(y_test, y_pred))
    print("\nClassification report:\n", classification_report(y_test, y_pred, digits=3))


    eval_df = df_test[[GROUP_COL]].copy()
    eval_df["y"] = y_test
    eval_df["p"] = proba_cal
    eval_df = eval_df.reset_index(drop=True)
    aucs, skipped = [], 0
    for g, grp in eval_df.groupby(GROUP_COL):
        if grp["y"].nunique() < 2:
            skipped += 1
            continue
        aucs.append(roc_auc_score(grp["y"].values, grp["p"].values))
    if aucs:
        macro_auc = float(np.mean(aucs))
        print(f"ROC AUC macro par accumulation: {macro_auc:.5f}  "
              f"(groupes utilisés={len(aucs)}, ignorés={skipped})")
    else:
        print("ROC AUC macro par accumulation: impossible (aucun groupe avec 2 classes)")

    # oos_break = df_test[T_SINCE_COL] == 0
    # if oos_break.any():
    #     proba_b0 = proba_cal[oos_break]; y_b0 = y_test[oos_break]
    #     y_pred_b0 = (proba_b0 >= 0.5).astype(int)
    #     print("\n=== OOS (snapshot au break, t=0) ===")
    #     print("ROC AUC :", roc_auc_score(y_b0, proba_b0))
    #     print("PR  AUC :", average_precision_score(y_b0, proba_b0))
    #     print("Confusion matrix (thr=0.5):\n", confusion_matrix(y_b0, y_pred_b0))
    #     print("\nClassification report:\n", classification_report(y_b0, y_pred_b0, digits=3))

    dump({
        "preprocessor": preproc,
        "booster_params": final_params,
        "best_rounds": best_rounds,
        "calibrator": iso,
        "features": FEATURES,
        "time_col": TIME_COL, "group_col": GROUP_COL, "target_col": TARGET_COL, "t_since_col": T_SINCE_COL,
        "cv_history": cv_history
    }, MODEL_OUT)
    booster.save_model(BOOSTER_JSON_OUT)
    print(f"\nModèle sauvegardé -> {MODEL_OUT}")
    print(f"Booster JSON -> {BOOSTER_JSON_OUT}")

    out = df_test[[GROUP_COL, TIME_COL, T_SINCE_COL]].copy()
    out["y_true"] = y_test; out["proba_tp"] = proba_cal; out["pred"] = y_pred
    out.to_csv(PREDICTIONS_OUT, index=False)
    print(f"Prédictions OOS -> {PREDICTIONS_OUT}")

    end = datetime.now()
    print("Durée totale :", end - start)

if __name__ == "__main__":
    main()

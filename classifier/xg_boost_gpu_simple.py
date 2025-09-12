# train_dynamic_breakout_classifier_xgb_cv_simple.py
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
TIME_COL, TARGET_COL= "timestamp", "y"
TEST_RATIO = 0.2
N_SPLITS = 5
RANDOM_SEED = 42
os.makedirs("data", exist_ok=True)
MODEL_OUT = "data/breakout_model_dynamic_gpu_cv.joblib"
BOOSTER_JSON_OUT = "data/breakout_model_dynamic_gpu_cv.json"
PREDICTIONS_OUT = "data/oos_predictions_dynamic_gpu_cv.csv"

N_TRIALS = 12
NUM_BOOST_ROUND = 3000
EARLY_STOPPING_ROUNDS = 150

# -------- UTILS --------
def load_df(path):
    df = pd.read_csv(path, parse_dates=[TIME_COL])
    df = df.replace([np.inf, -np.inf], np.nan)
    return df.sort_values([TIME_COL]).reset_index(drop=True)

def check_columns(df, feats):
    required = {TIME_COL, TARGET_COL}
    miss = required - set(df.columns)
    if miss:
        raise ValueError(f"Colonnes manquantes: {miss}")
    for f in feats:
        if f not in df.columns:
            raise ValueError(f"Feature manquante: {f}")

def split_holdout(df, ratio=0.2):
    n_test = max(1, int(len(df) * ratio))
    mask_test = np.zeros(len(df), dtype=bool)
    mask_test[-n_test:] = True
    return ~mask_test, mask_test

def make_time_cv_splits(df, n_splits=5):
    n = len(df)
    fold_sizes = np.full(n_splits, n // n_splits, dtype=int)
    fold_sizes[: n % n_splits] += 1
    bounds = np.cumsum(fold_sizes)
    splits = []
    for i in range(1, n_splits):
        tr = np.arange(bounds[i-1])
        va = np.arange(bounds[i-1], bounds[i])
        splits.append((tr, va))
    return splits

def filter_cv_splits_with_both_classes(cv_splits, y_all):
    safe = []
    for tr, va in cv_splits:
        if np.unique(y_all[va]).size == 2:
            safe.append((tr, va))
    if not safe:
        raise ValueError("Aucun pli de CV n'a les deux classes.")
    return safe

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
    return params

# -------- MAIN --------
def main():
    start = datetime.now()
    rng = random.Random(RANDOM_SEED)
    np.random.seed(RANDOM_SEED)

    df = load_df(CSV_PATH)
    FEATURES = [c for c in df.columns if c.startswith("f_")]
    # FEATURES = ['f_timestamp']
    check_columns(df, FEATURES)
    X = df[FEATURES]; y = df[TARGET_COL].astype(int).values

    mask_tr, mask_te = split_holdout(df, TEST_RATIO)
    X_train, y_train = X.loc[mask_tr], y[mask_tr]
    X_test,  y_test  = X.loc[mask_te], y[mask_te]
    df_train, df_test = df.loc[mask_tr], df.loc[mask_te]

    cv_raw = make_time_cv_splits(df_train, n_splits=N_SPLITS)
    cv_splits = filter_cv_splits_with_both_classes(cv_raw, y_train)

    # --- prétraitement ---
    preproc = ColumnTransformer([("num", SimpleImputer(strategy="median"), FEATURES)])
    Xtr_pre = preproc.fit_transform(X_train)
    if hasattr(Xtr_pre, "toarray"):
        Xtr_pre = Xtr_pre.toarray()
    Xtr_pre = Xtr_pre.astype(np.float32, copy=False)

    dtrain = xgb.DMatrix(Xtr_pre, label=y_train)

    monotonic = None
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
            folds=cv_splits,
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
    if hasattr(Xte_pre, "toarray"):
        Xte_pre = Xte_pre.toarray()
    Xte_pre = Xte_pre.astype(np.float32, copy=False)
    dtest = xgb.DMatrix(Xte_pre)
    proba = booster.predict(dtest)
    proba_cal = iso.transform(proba)
    y_pred = (proba_cal >= 0.5).astype(int)

    print("\n=== OOS ===")
    print(f"Instances de test : {len(y_test)}")
    print("ROC AUC :", roc_auc_score(y_test, proba_cal))
    print("PR (Precision) AUC :", average_precision_score(y_test, proba_cal))
    print("\nConfusion matrix (thr=0.5):\n", confusion_matrix(y_test, y_pred))
    print("\nClassification report:\n", classification_report(y_test, y_pred, digits=3))

    dump({
        "preprocessor": preproc,
        "booster_params": final_params,
        "best_rounds": best_rounds,
        "calibrator": iso,
        "features": FEATURES,
        "time_col": TIME_COL,
        "target_col": TARGET_COL,
        "cv_history": cv_history
    }, MODEL_OUT)
    booster.save_model(BOOSTER_JSON_OUT)
    print(f"\nModèle sauvegardé -> {MODEL_OUT}")
    print(f"Booster JSON -> {BOOSTER_JSON_OUT}")

    out = df_test[[TIME_COL]].copy()
    out["y_true"] = y_test
    out["proba_tp"] = proba_cal
    out["pred"] = y_pred
    out.to_csv(PREDICTIONS_OUT, index=False)
    print(f"Prédictions OOS -> {PREDICTIONS_OUT}")

    end = datetime.now()
    print("Durée totale :", end - start)

if __name__ == "__main__":
    main()

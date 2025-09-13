# train_dynamic_breakout_classifier_xgb_cv_simple_twocsv.py
import warnings, os, random
warnings.filterwarnings("ignore", message=".*Falling back to prediction using DMatrix.*")

from datetime import datetime
import numpy as np
import pandas as pd
from joblib import dump
from itertools import product
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.metrics import roc_auc_score, average_precision_score, classification_report, confusion_matrix, \
    precision_recall_curve
from sklearn.isotonic import IsotonicRegression
import xgboost as xgb
from packaging import version
import matplotlib.pyplot as plt

# -------- CONFIG --------
TRAIN_CSV_PATH = "/data/trading_ml/classifier_train.csv"   # <--- fichier TRAIN/VALID
TEST_CSV_PATH  = "/data/trading_ml/classifier_test.csv"    # <--- fichier TEST (vraiment OOS)

TIME_COL, TARGET_COL = "timestamp", "y"
N_SPLITS = 5
RANDOM_SEED = 42
CALIBRATION_RATIO = 0.2  # part finale du TRAIN réservée à la calibration isotone (pas le TEST)
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

def split_tail(df, ratio=0.2):
    n_hold = max(1, int(len(df) * ratio))
    mask_hold = np.zeros(len(df), dtype=bool)
    mask_hold[-n_hold:] = True
    return ~mask_hold, mask_hold

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

    # --- charge 2 fichiers séparés ---
    df_trval = load_df(TRAIN_CSV_PATH)
    df_test  = load_df(TEST_CSV_PATH)

    # features définies depuis le TRAIN uniquement
    # FEATURES= ['f_timestamp']
    FEATURES = [c for c in df_trval.columns if c.startswith("f_")]
    # FEATURES = [c for c in df_trval.columns if c.startswith("o_")]
    # FEATURES += [c for c in df_trval.columns if c.startswith("c_")]
    # FEATURES = ['f_timestamp']
    check_columns(df_trval, FEATURES)

    # s'assure que le TEST a bien y et timestamp
    for c in [TIME_COL, TARGET_COL]:
        if c not in df_test.columns:
            raise ValueError(f"Colonne '{c}' manquante dans le fichier TEST.")
    # ajoute les features manquantes du TEST (remplies NaN -> imputées)
    missing_in_test = [c for c in FEATURES if c not in df_test.columns]
    if missing_in_test:
        print(f"⚠️ Colonnes absentes dans le TEST: {missing_in_test} -> remplies par NaN.")
        for c in missing_in_test:
            df_test[c] = np.nan

    # garde un segment de calibration à la fin du TRAIN (temporellement)
    mask_core, mask_cal = split_tail(df_trval, ratio=CALIBRATION_RATIO)
    df_core = df_trval.loc[mask_core].reset_index(drop=True)   # pour CV + entraînement final
    df_cal  = df_trval.loc[mask_cal].reset_index(drop=True)    # pour isotonic regression

    # contrôle d'overlap temporel simple entre TRAIN et TEST (recommandé)
    if len(df_core) and len(df_test):
        if df_test[TIME_COL].min() <= df_core[TIME_COL].max():
            print("⚠️ Attention: TRAIN et TEST se recouvrent dans le temps. Vérifie tes fichiers.")

    # --- matrices ---
    X_core = df_core[FEATURES]
    y_core = df_core[TARGET_COL].astype(int).values
    X_cal  = df_cal[FEATURES]
    y_cal  = df_cal[TARGET_COL].astype(int).values
    X_test = df_test[FEATURES]
    y_test = df_test[TARGET_COL].astype(int).values

    # --- CV time-series sur le segment core uniquement ---
    cv_raw = make_time_cv_splits(df_core, n_splits=N_SPLITS)
    cv_splits = filter_cv_splits_with_both_classes(cv_raw, y_core)

    # --- prétraitement (fit uniquement sur core) ---
    preproc = ColumnTransformer([("num", SimpleImputer(strategy="median"), FEATURES)])
    Xcore_pre = preproc.fit_transform(X_core)
    if hasattr(Xcore_pre, "toarray"):
        Xcore_pre = Xcore_pre.toarray()
    Xcore_pre = Xcore_pre.astype(np.float32, copy=False)

    dcore = xgb.DMatrix(Xcore_pre, label=y_core, missing=np.nan)

    base = gpu_params(monotonic=None)

    # --- mini HPO via xgb.cv ---
    trials = choose_param_trials(rng)
    best_auc, best_params, best_rounds = -np.inf, None, None
    cv_history = []

    for i, hp in enumerate(trials, 1):
        params = {**base, **hp}
        print(f"\n[CV {i}/{len(trials)}] {hp}")
        cvres = xgb.cv(
            params=params,
            dtrain=dcore,
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

    # --- entraînement final sur tout 'core' ---
    final_params = {**base, **best_params}
    booster = xgb.train(params=final_params, dtrain=dcore, num_boost_round=best_rounds)

    # --- calibration isotone sur le bloc 'cal' (séparé du core et du test) ---
    Xcal_pre = preproc.transform(X_cal)
    if hasattr(Xcal_pre, "toarray"):
        Xcal_pre = Xcal_pre.toarray()
    Xcal_pre = Xcal_pre.astype(np.float32, copy=False)
    dcal = xgb.DMatrix(Xcal_pre, missing=np.nan)
    proba_calfit = booster.predict(dcal)
    iso = IsotonicRegression(out_of_bounds="clip").fit(proba_calfit, y_cal)

    # --- évaluation OOS (vrai TEST) ---
    Xte_pre = preproc.transform(X_test)
    if hasattr(Xte_pre, "toarray"):
        Xte_pre = Xte_pre.toarray()
    Xte_pre = Xte_pre.astype(np.float32, copy=False)
    dtest = xgb.DMatrix(Xte_pre, missing=np.nan)
    proba = booster.predict(dtest)
    proba_cal = iso.transform(proba)

    # R = df_test['f_rr'].astype(float).values
    # R = np.clip(R, 1e-6, None)
    # thr = 1.0 / (R + 1.0)
    # y_pred = (proba_cal >= thr).astype(int)
    y_pred = (proba_cal >= 0.5).astype(int)

    print("\n=== OOS (TEST) ===")
    print(f"Instances de test : {len(y_test)}")
    try:
        print("ROC AUC :", roc_auc_score(y_test, proba_cal))
    except ValueError as e:
        print("ROC AUC : non défini (probablement une seule classe dans le TEST) ->", e)
    pr_auc = average_precision_score(y_test, proba_cal)
    print("PR (Precision) AUC :", pr_auc)
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

    out = df_test[[TIME_COL ,'id']].copy()
    out["y_true"] = y_test
    out["proba_tp"] = proba_cal
    out["pred"] = y_pred
    out.to_csv(PREDICTIONS_OUT, index=False)
    print(f"Prédictions OOS -> {PREDICTIONS_OUT}")

    prec, rec, thresh = precision_recall_curve(y_test, proba_cal)

    plt.plot(rec, prec, label=f"PR AUC = {pr_auc:.3f}")
    plt.xlabel("Recall")
    plt.ylabel("Precision")
    plt.legend()
    plt.show()

    end = datetime.now()
    print("Durée totale :", end - start)

if __name__ == "__main__":
    main()

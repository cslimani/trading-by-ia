# train_dynamic_breakout_classifier_xgb_gpuB.py
# Multi-lignes par accumulation : 1 ligne par barre entre break et TP/SL.
# Colonnes attendues au minimum :
#  - accum_id      : identifiant de l'accumulation (groupe)
#  - timestamp     : datetime
#  - t_since_break : 0 au break, 1 la barre suivante, etc.
#  - y             : 1 si TP touche en premier (depuis le break), 0 si SL
#  - vos features  : accum_size, ema_20, rsi_14, atr_break, dist_tp, dist_sl, ...

import warnings
warnings.filterwarnings("ignore", message=".*Falling back to prediction using DMatrix.*")
from datetime import datetime
import numpy as np
import pandas as pd
from joblib import dump
from sklearn.model_selection import RandomizedSearchCV
from sklearn.pipeline import Pipeline
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.metrics import (
    roc_auc_score, average_precision_score,
    classification_report, confusion_matrix
)
from sklearn.isotonic import IsotonicRegression
import xgboost as xgb

# -------------- CONFIG --------------
CSV_PATH = "/data/trading_ml/bars_after_break.csv"
TIME_COL = "timestamp"
GROUP_COL = "accum_id"
TARGET_COL = "y"
T_SINCE_COL = "t_since_break"

TEST_GROUP_RATIO = 0.2          # dernier 20% des accumulations (par temps) en holdout
N_SPLITS = 5                    # pour la CV sur les groupes
MODEL_OUT = "data/breakout_model_dynamic.joblib"
PREDICTIONS_OUT = "data/oos_predictions_dynamic.csv"

# -------------- UTILS --------------
def load_df(path):
    df = pd.read_csv(path, parse_dates=[TIME_COL])
    df = df.replace([np.inf, -np.inf], np.nan)
    # on force un index séquentiel pour que les splits par indices fonctionnent
    return df.sort_values([TIME_COL, GROUP_COL, T_SINCE_COL]).reset_index(drop=True)

def audit_splits(df, splits):
    ok = True
    for k, (tr, va) in enumerate(splits, 1):
        gtr = set(df.iloc[tr][GROUP_COL])
        gva = set(df.iloc[va][GROUP_COL])
        # 1) aucun id partagé
        if gtr & gva:
            print(f"[Fold {k}] Ids partagés:", gtr & gva); ok = False
        # 2) ordre temporel (approx) :
        t_tr_max = df.iloc[tr][TIME_COL].max()
        t_va_min = df.iloc[va][TIME_COL].min()
        if t_tr_max >= t_va_min:
            print(f"[Fold {k}] Train déborde sur la val (t_train_max >= t_val_min).")
            ok = False
    return ok

def check_columns(df, _features):
    required = {GROUP_COL, TIME_COL, TARGET_COL, T_SINCE_COL}
    missing = required - set(df.columns)
    if missing:
        raise ValueError(f"Colonnes manquantes: {missing}")
    for f in _features:
        if f not in df.columns:
            raise ValueError(f"Feature manquante: {f}")

def make_group_order(df):
    # ordre chronologique des groupes par première apparition
    first_t = df.groupby(GROUP_COL)[TIME_COL].min().sort_values()
    return list(first_t.index)

def split_holdout_by_group(df, test_group_ratio=0.2):
    grp_order = make_group_order(df)
    n_test = max(1, int(len(grp_order) * test_group_ratio))
    test_groups = set(grp_order[-n_test:])
    mask_test = df[GROUP_COL].isin(test_groups)
    return (~mask_test).to_numpy(), mask_test.to_numpy()

def make_group_time_cv_splits(df, n_splits=5):
    """
    Génère des folds temporels par groupes :
    fold i : train = groupes jusqu'à i-1, val = groupe i
    """
    grp_order = make_group_order(df)
    if n_splits > len(grp_order):
        n_splits = len(grp_order)
    # On découpe grp_order en n_splits blocs contigus (par temps)
    sizes = np.full(n_splits, len(grp_order)//n_splits, dtype=int)
    sizes[: len(grp_order) % n_splits] += 1
    bounds = np.cumsum(sizes)

    splits = []
    for i in range(1, n_splits):  # on a besoin d'un train non vide
        train_groups = set(grp_order[:bounds[i-1]])
        val_groups = set(grp_order[bounds[i-1]:bounds[i]])
        train_idx = df.index[df[GROUP_COL].isin(train_groups)].to_numpy()
        val_idx = df.index[df[GROUP_COL].isin(val_groups)].to_numpy()
        splits.append((train_idx, val_idx))
    return splits

def make_weights(df, y):
    """
    Évite qu'une longue séquence pèse trop :
    - poids de groupe = 1 / longueur de séquence
    - équilibrage classes (0.5 / fréquence)
    """
    seq_len = df.groupby(GROUP_COL).size()
    w_group = df[GROUP_COL].map(1.0 / seq_len)
    p = y.mean()
    w_pos = 0.5 / p if p > 0 else 1.0
    w_neg = 0.5 / (1 - p) if p < 1 else 1.0
    w_class = np.where(y == 1, w_pos, w_neg)
    return (w_group.values * w_class).astype(float)

def filter_cv_splits_with_both_classes(cv_splits, y_all):
    safe = []
    for tr, va in cv_splits:
        y_va = y_all[va]
        if np.unique(y_va).size == 2:
            safe.append((tr, va))
    if not safe:
        raise ValueError("Aucun pli de CV n'a les deux classes. Revoir la stratégie de split.")
    return safe

def permute_train_within_group_local(y_train, groups_train, rng):
    y_perm = y_train.copy()
    for g in np.unique(groups_train):
        idx = np.where(groups_train == g)[0]
        if idx.size > 1:
            y_perm[idx] = rng.permutation(y_perm[idx])
    return y_perm

# --- helpers GPU pour prédire depuis le pipeline scikit entraîné ---
def predict_proba_gpu_from_pipeline(fitted_pipeline, X_df):
    """
    Transforme X_df via fitted_pipeline['prep'] (CPU),
    construit un DMatrix, et appelle Booster.predict.
    Le booster ayant été entraîné avec device='cuda',
    la prédiction s'exécute sur GPU (sans warning).
    Retourne un np.ndarray (CPU) de proba classe 1.
    """
    import numpy as np
    import xgboost as xgb

    prep = fitted_pipeline.named_steps["prep"]
    model = fitted_pipeline.named_steps["model"]
    booster = model.get_booster()

    X_pre = prep.transform(X_df)                  # numpy/sparse CPU
    if hasattr(X_pre, "toarray"):
        X_pre = X_pre.toarray()
    X_pre = X_pre.astype(np.float32, copy=False)

    dtest = xgb.DMatrix(X_pre)                    # DMatrix côté CPU
    proba = booster.predict(dtest)                # exécution sur GPU (device='cuda')
    return proba

# -------------- MAIN --------------
def main():
    df = load_df(CSV_PATH)
    FEATURES = [
        "risk_ratio",
        "accum_size",
        "range_swing_high_ratio",
        "t_since_break",
        "swing_high_distance",
    ]
    # FEATURES += [c for c in df.columns if c.startswith("o_")]
    # FEATURES += [c for c in df.columns if c.startswith("h_")]
    # FEATURES += [c for c in df.columns if c.startswith("l_")]
    # FEATURES += [c for c in df.columns if c.startswith("c_")]
    # FEATURES += [c for c in df.columns if c.startswith("v_")]
    n_features = len(FEATURES)
    check_columns(df, FEATURES)

    X = df[FEATURES]
    y = df[TARGET_COL].astype(int).values

    # Holdout par groupes (derniers groupes en temps)
    mask_tr, mask_te = split_holdout_by_group(df, TEST_GROUP_RATIO)
    X_train, y_train = X.loc[mask_tr], y[mask_tr]
    X_test,  y_test  = X.loc[mask_te], y[mask_te]
    df_train, df_test = df.loc[mask_tr], df.loc[mask_te]

    # Préprocessing : imputation uniquement (XGBoost gère aussi les NaN, mais on garde l'équivalence)
    preproc = ColumnTransformer([
        ("num", SimpleImputer(strategy="median"), FEATURES)
    ])

    # Modèle XGBoost (GPU)
    base = xgb.XGBClassifier(
        tree_method="hist",
        device="cuda",
        eval_metric="logloss",
        random_state=42,
        n_jobs=-1
    )

    # (optionnel) Contraintes monotoniques
    monotonic = [-1] + [0]*(n_features-1)
    # base.set_params(monotone_constraints="(" + ",".join(map(str, monotonic)) + ")")

    pipe = Pipeline([
        ("prep", preproc),
        ("model", base)
    ])

    # Grille adaptée à XGBoost
    param_grid = {
        "model__learning_rate": [0.03, 0.05, 0.1],
        "model__n_estimators": [200, 400, 800],
        "model__max_depth": [3, 5, 7, 9],
        "model__min_child_weight": [1, 5, 10, 20],
        "model__subsample": [0.6, 0.8, 1.0],
        "model__colsample_bytree": [0.6, 0.8, 1.0],
        "model__gamma": [0, 0.1, 0.5, 1.0],
        "model__reg_lambda": [1.0, 5.0, 10.0],
        "model__reg_alpha": [0.0, 0.1, 1.0],
    }

    # Splits de CV temporels par groupes
    cv_raw = make_group_time_cv_splits(df_train, n_splits=N_SPLITS)
    cv_splits = filter_cv_splits_with_both_classes(cv_raw, y_train)

    if not audit_splits(df_train, cv_splits):
        raise ValueError("Problème dans les splits de CV. Abandon.")

    # Pondérations (groupe + classe)
    w_train = make_weights(df_train, y_train)

    # --- Recherche aléatoire (peut afficher des warnings, masqués ci-dessus) ---
    search = RandomizedSearchCV(
        pipe,
        param_distributions=param_grid,
        n_iter=20,
        scoring="roc_auc",
        cv=cv_splits,              # iterable (train_idx, val_idx)
        n_jobs=-1, verbose=1, random_state=42
    )
    # IMPORTANT : transmet les poids via le nom de l’étape
    search.fit(X_train, y_train, **{"model__sample_weight": w_train})

    print("\nMeilleurs hyperparamètres :", search.best_params_)
    best = search.best_estimator_

    # -------- Calibration isotone "maison" --------
    # On génère des proba TRAIN en full GPU (bypass pipeline.predict_proba pour éviter le warning)
    proba_tr = predict_proba_gpu_from_pipeline(best, X_train)  # numpy (CPU)
    iso = IsotonicRegression(out_of_bounds="clip")
    iso.fit(proba_tr, y_train)

    # -------- Évaluation OOS (full GPU) --------
    proba = predict_proba_gpu_from_pipeline(best, X_test)  # numpy (CPU) après GPU inf
    proba_cal = iso.transform(proba)
    y_pred = (proba_cal >= 0.5).astype(int)

    print("\n=== OOS (toutes lignes) ===")
    print(f"Instances de test : {len(y_test)}")
    print("ROC AUC :", roc_auc_score(y_test, proba_cal))
    print("PR  AUC :", average_precision_score(y_test, proba_cal))
    print("\nConfusion matrix (thr=0.5):\n", confusion_matrix(y_test, y_pred))
    print("\nClassification report:\n", classification_report(y_test, y_pred, digits=3))

    # ---- Évaluation OOS sur le snapshot du break (t=0) ----
    oos_break = df_test[T_SINCE_COL] == 0
    if oos_break.any():
        proba_b0 = proba_cal[oos_break]
        y_b0 = y_test[oos_break]
        y_pred_b0 = (proba_b0 >= 0.5).astype(int)
        print("\n=== OOS (snapshot au break, t=0) ===")
        print("ROC AUC :", roc_auc_score(y_b0, proba_b0))
        print("PR  AUC :", average_precision_score(y_b0, proba_b0))
        print("Confusion matrix (thr=0.5):\n", confusion_matrix(y_b0, y_pred_b0))
        print("\nClassification report:\n", classification_report(y_b0, y_pred_b0, digits=3))

    # Sauvegardes
    dump({
        # On garde le pipeline scikit pour le prétraitement + modèle GPU
        "pipeline": best,
        # Calibrateur isotone appris en CPU
        "calibrator": iso,
        "features": FEATURES,
        "time_col": TIME_COL,
        "group_col": GROUP_COL,
        "target_col": TARGET_COL,
        "t_since_col": T_SINCE_COL,
        "best_params": search.best_params_
    }, MODEL_OUT)
    print(f"\nModèle sauvegardé -> {MODEL_OUT}")

    out = df_test[[GROUP_COL, TIME_COL, T_SINCE_COL]].copy()
    out["y_true"] = y_test
    out["proba_tp"] = proba_cal
    out["pred"] = y_pred
    out.to_csv(PREDICTIONS_OUT, index=False)
    print(f"Prédictions OOS -> {PREDICTIONS_OUT}")

if __name__ == "__main__":
    start = datetime.now()
    main()
    end = datetime.now()
    print("Durée totale :", end - start)

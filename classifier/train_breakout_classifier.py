# train_dynamic_breakout_classifier.py
# Multi-lignes par accumulation : 1 ligne par barre entre break et TP/SL.
# Colonnes attendues au minimum :
#  - accum_id      : identifiant de l'accumulation (groupe)
#  - timestamp     : datetime
#  - t_since_break : 0 au break, 1 la barre suivante, etc.
#  - y             : 1 si TP touche en premier (depuis le break), 0 si SL
#  - vos features  : accum_size, ema_20, rsi_14, atr_break, dist_tp, dist_sl, ...

import numpy as np
import pandas as pd
from joblib import dump
from sklearn.model_selection import RandomizedSearchCV
from sklearn.pipeline import Pipeline
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler
from sklearn.impute import SimpleImputer
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.calibration import CalibratedClassifierCV
from sklearn.metrics import (
    roc_auc_score, average_precision_score,
    classification_report, confusion_matrix
)

# -------------- CONFIG --------------
CSV_PATH = "/data/trading_ml/bars_after_break.csv"
TIME_COL = "timestamp"
GROUP_COL = "accum_id"
TARGET_COL = "y"
T_SINCE_COL = "t_since_break"

FEATURES = [
    "accum_size",
    # "ema_20",
    # "ema_50",
    # "rsi_14",
    # "atr_break",
    # "dist_tp", "dist_sl",
    # ... ajoute tes features ici
]

TEST_GROUP_RATIO = 0.2          # dernier 20% des accumulations (par temps) en holdout
N_SPLITS = 5                    # pour la CV sur les groupes
MODEL_OUT = "breakout_model_dynamic.joblib"
PREDICTIONS_OUT = "oos_predictions_dynamic.csv"

# -------------- UTILS --------------
def load_df(path):
    df = pd.read_csv(path, parse_dates=[TIME_COL])
    df = df.replace([np.inf, -np.inf], np.nan)
    # on force un index séquentiel pour que les splits par indices fonctionnent
    return df.sort_values([TIME_COL, GROUP_COL, T_SINCE_COL]).reset_index(drop=True)

def check_columns(df):
    required = {GROUP_COL, TIME_COL, TARGET_COL, T_SINCE_COL}
    missing = required - set(df.columns)
    if missing:
        raise ValueError(f"Colonnes manquantes: {missing}")
    for f in FEATURES:
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

# -------------- MAIN --------------
def main():
    df = load_df(CSV_PATH)
    check_columns(df)

    X = df[FEATURES]
    y = df[TARGET_COL].astype(int).values

    # Holdout par groupes (derniers groupes en temps)
    mask_tr, mask_te = split_holdout_by_group(df, TEST_GROUP_RATIO)
    X_train, y_train = X.iloc[mask_tr], y[mask_tr]
    X_test,  y_test  = X.iloc[mask_te], y[mask_te]
    df_train, df_test = df.iloc[mask_tr], df.iloc[mask_te]

    # Préprocessing
    preproc = ColumnTransformer([
        ("num", Pipeline([
            ("imp", SimpleImputer(strategy="median")),
            ("scaler", StandardScaler())
        ]), FEATURES)
    ])

    base = GradientBoostingClassifier(random_state=42)

    pipe = Pipeline([
        ("prep", preproc),
        ("model", base)
    ])

    param_grid = {
        "model__n_estimators": [200, 400, 600],
        "model__learning_rate": [0.02, 0.05, 0.1],
        "model__max_depth": [2, 3, 4],
        "model__subsample": [0.6, 0.8, 1.0],
        "model__min_samples_leaf": [20, 50, 100]
    }

    # Splits de CV temporels par groupes
    cv_splits = make_group_time_cv_splits(df_train, n_splits=N_SPLITS)

    # Pondérations (groupe + classe)
    w_train = make_weights(df_train, y_train)

    search = RandomizedSearchCV(
        pipe,
        param_distributions=param_grid,
        n_iter=20,
        scoring="roc_auc",
        cv=cv_splits,              # iterable (train_idx, val_idx)
        n_jobs=-1, verbose=1, random_state=42
    )
    search.fit(X_train, y_train, **{"model__sample_weight": w_train})

    print("\nMeilleurs hyperparamètres :", search.best_params_)
    best = search.best_estimator_

    # Calibration
    cal = CalibratedClassifierCV(best, method="isotonic", cv=3)
    cal.fit(X_train, y_train, sample_weight=w_train)

    # ---- Évaluation OOS (toutes lignes) ----
    proba = cal.predict_proba(X_test)[:, 1]
    y_pred = (proba >= 0.5).astype(int)
    print("\n=== OOS (toutes lignes) ===")
    print("ROC AUC :", roc_auc_score(y_test, proba))
    print("PR  AUC :", average_precision_score(y_test, proba))
    print("\nConfusion matrix (thr=0.5):\n", confusion_matrix(y_test, y_pred))
    print("\nClassification report:\n", classification_report(y_test, y_pred, digits=3))

    # ---- Évaluation OOS sur le snapshot du break (t=0) ----
    oos_break = df_test[T_SINCE_COL] == 0
    if oos_break.any():
        proba_b0 = proba[oos_break]
        y_b0 = y_test[oos_break]
        y_pred_b0 = (proba_b0 >= 0.5).astype(int)
        print("\n=== OOS (snapshot au break, t=0) ===")
        print("ROC AUC :", roc_auc_score(y_b0, proba_b0))
        print("PR  AUC :", average_precision_score(y_b0, proba_b0))
        print("Confusion matrix (thr=0.5):\n", confusion_matrix(y_b0, y_pred_b0))
        print("\nClassification report:\n", classification_report(y_b0, y_pred_b0, digits=3))

    # Sauvegardes
    dump({
        "model": cal,
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
    out["proba_tp"] = proba
    out["pred"] = y_pred
    out.to_csv(PREDICTIONS_OUT, index=False)
    print(f"Prédictions OOS -> {PREDICTIONS_OUT}")

if __name__ == "__main__":
    main()

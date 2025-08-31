# prep_timesfm_ranges.py
import pandas as pd
import numpy as np
from pathlib import Path

"""
Entrées:
  - Un DataFrame OHLCV (au minimum colonnes: ['timestamp','close'])
  - Une liste de ranges: chaque range est (start_idx, end_idx) d'un segment d'accumulation
Paramètres:
  ctx_len: longueur de contexte fournie au modèle (bougies dans le range)
  horizon: nombre de bougies à prédire après le range
Sortie:
  - Un CSV multi-séries au format TimesFM: colonnes ['unique_id','ds','y']
    Chaque "unique_id" correspond à une fenêtre (range) et la série contient
    (contexte normalisé) suivi des cibles (toujours utiles pour contrôle offline).
"""

def make_windows_from_ranges(df, ranges, ctx_len=256, horizon=64, freq_alias="MIN"):
    rows = []
    uid = 0
    for (s, e) in ranges:
        # contexte = bougies du range (si e - s + 1 >= ctx_len, on prend la fin)
        if e - s + 1 < ctx_len: 
            continue
        ctx_start = e - ctx_len + 1
        ctx = df.iloc[ctx_start:e+1].copy()  # [ctx_len] points
        # cibles = N bougies après le range
        target_start = e + 1
        target_end = e + horizon
        if target_end >= len(df):
            continue
        tgt = df.iloc[target_start:target_end+1].copy()  # [horizon] points

        # normalisation par la hauteur du range (max-min sur le CONTEXTE)
        lo = ctx['close'].min()
        hi = ctx['close'].max()
        h = hi - lo
        if h <= 0:
            continue
        ctx_norm = (ctx['close'].values - lo) / h
        tgt_norm = (tgt['close'].values - lo) / h  # même échelle

        # construire une "série" univariée concaténée (contexte+futur) pour sauvegarde
        ser = np.concatenate([ctx_norm, tgt_norm])
        ts = pd.to_datetime(list(ctx['timestamp']) + list(tgt['timestamp']))

        # TimesFM aime un dataframe long: unique_id, ds, y
        uid += 1
        rows.extend([{"unique_id": f"R{uid}", "ds": ts[i], "y": float(ser[i])} 
                     for i in range(len(ser))])
    out = pd.DataFrame(rows).sort_values(["unique_id","ds"])
    # Hint: TimesFM comprend la fréquence via 'freq' côté API/CLI (ex: "MIN","H","D","M"...)
    return out

if __name__ == "__main__":
    # EXEMPLE: charge tes données (à adapter)
    df = pd.read_csv("prices.csv")  # colonnes: timestamp, open, high, low, close, volume
    df['timestamp'] = pd.to_datetime(df['timestamp'], utc=True)

    # Exemple: ranges détectés ailleurs (start_idx, end_idx)
    # Remplace par ta propre détection d'accumulations
    ranges = [(1000, 1200), (5000, 5300), (9000, 9200)]

    ctx_len = 256
    horizon = 64
    out = make_windows_from_ranges(df, ranges, ctx_len=ctx_len, horizon=horizon, freq_alias="MIN")
    Path("data").mkdir(exist_ok=True)
    out.to_csv("data/train_ranges_norm.csv", index=False)
    print(out.head())

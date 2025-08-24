from __future__ import annotations
from datetime import datetime, timedelta, timezone
import MetaTrader5 as mt5
import pandas as pd
from typing import Dict, Literal, Tuple

from db_utils import insertPrices

PriceBasis = Literal["bid", "ask", "mid"]
# TIME_RANGE ="M1"
# TIME_RANGE_RESAMPLE="1min"
TIME_RANGE ="S30"
TIME_RANGE_RESAMPLE="30S"
market = "US100.cash"
start_state = datetime(2025, 1, 30, 0, 0, tzinfo=timezone.utc)
end_date = datetime(2025, 8, 20, 0, 0, tzinfo=timezone.utc)  # exclu

def ticks_to_ohlc_1min(
    ticks_df: pd.DataFrame,
    tf,
    price_basis: PriceBasis = "mid",
    fill_missing: bool = False
) -> pd.DataFrame:
    if ticks_df.empty:
        return pd.DataFrame(columns=["open", "high", "low", "close", "tick_volume"])

    # --- Horodatage (UTC) ---
    if "time_msc" in ticks_df.columns:
        ts = pd.to_datetime(ticks_df["time_msc"], unit="ms", utc=True)
    else:
        # fallback si seule la colonne 'time' (secondes) est présente
        ts = pd.to_datetime(ticks_df["time"], unit="s", utc=True)

    df = ticks_df.copy()
    df["ts"] = ts
    df = df.set_index("ts").sort_index()

    # --- Prix de référence (en éliminant les ticks incomplets) ---
    if price_basis == "bid":
        price = df["bid"].astype(float)
    elif price_basis == "ask":
        price = df["ask"].astype(float)
    else:  # "mid"
        # ne garder que les ticks où bid et ask existent
        df = df.copy()
        if "bid" not in df or "ask" not in df:
            raise KeyError("Colonnes 'bid' et 'ask' requises pour price_basis='mid'.")
        price = ((df["bid"].astype(float) + df["ask"].astype(float)) / 2.0)

    # drop des ticks sans prix (évite de propager des NaN dans le resample)
    price = price.dropna()

    # Si tout a été droppé (ex: trop de NaN en entrée)
    if price.empty:
        return pd.DataFrame(columns=["open", "high", "low", "close", "tick_volume"])

    # --- OHLC 1 minute ---
    ohlc = price.resample(tf).ohlc()
    ohlc = ohlc.dropna(how="any")

    # --- Volume / tick_volume ---
    # Selon ton DF MT5, on peut avoir 'volume' (somme) ou rien -> à défaut: nombre de ticks
    # if "volume" in df.columns:
    #     vol = df["volume"].reindex(price.index, method=None)  # s'aligne sur l'index des ticks retenus
    #     volTF = vol.resample("1min").sum(min_count=1)
    # else:
    # faute de mieux, on prend le nombre de ticks par minute
    volTF = price.resample(tf).count()

    # aligne sur l'index OHLC
    volTF = volTF.reindex(ohlc.index)
    ohlc["tick_volume"] = volTF

    if fill_missing:
        # minutes sans ticks -> bougie plate = prev_close
        prev_close = ohlc["close"].ffill()

        # masque des minutes vides (pas d'open/high/low/close)
        empty_minutes = ohlc["close"].isna()

        # remplissage des O/H/L/C avec le close précédent
        for col in ["open", "high", "low", "close"]:
            ohlc.loc[empty_minutes, col] = prev_close.loc[empty_minutes]

        # volume = 0 sur ces minutes "synthétiques"
        ohlc.loc[empty_minutes, "tick_volume"] = 0

        # supprimer les minutes avant le premier close connu (ffill n'a rien à propager)
        ohlc = ohlc.loc[prev_close.notna()]
    else:
        # Si on ne remplit pas, on peut au moins mettre volume=0 là où il n'y a pas eu de ticks
        ohlc["tick_volume"] = ohlc["tick_volume"].fillna(0)

    # Optionnel: garantir types propres
    ohlc[["open", "high", "low", "close"]] = ohlc[["open", "high", "low", "close"]].astype(float)
    ohlc["tick_volume"] = ohlc["tick_volume"].astype(float)

    return ohlc

def copy_ticks_by_hour(symbol: str,
                       start_utc: datetime,
                       end_utc: datetime,
                       tf,
                       price_basis: PriceBasis = "mid",
                       fill_missing: bool = False,
                       flags: int = None
                      ) -> Tuple[Dict[pd.Timestamp, pd.DataFrame], pd.DataFrame]:
    """
    Boucle heure par heure de start_utc (inclus) à end_utc (exclu),
    copie les ticks via MT5 et agrège en bougies 1 minute.

    Retourne: (mapping_heure -> DF_1min, DF_global_concaténé)

    Notes:
    - start_utc et end_utc doivent être timezone-aware en UTC.
    - copy_ticks_range peut renvoyer jusqu'à ~1e6 ticks; si une heure dépasse, réduire l?intervalle.
    """
    if start_utc.tzinfo is None or end_utc.tzinfo is None:
        raise ValueError("start_utc et end_utc doivent être timezone-aware (UTC).")
    if start_utc >= end_utc:
        raise ValueError("start_utc doit être strictement avant end_utc.")

    if flags is None:
        flags = mt5.COPY_TICKS_ALL

    if not mt5.initialize():
        code, desc = mt5.last_error()
        raise RuntimeError(f"MT5 init failed: {code} {desc}")

    try:
        # fenêtre d'une heure
        step = timedelta(hours=1)
        cur = start_utc

        while cur < end_utc:
            hour_start = cur
            hour_end = min(cur + step, end_utc)
            print(f"Inserting prices from {hour_start} to {hour_end}")

            # On récupère les ticks sur [hour_start, hour_end)
            ticks = mt5.copy_ticks_range(symbol, hour_start, hour_end, flags)

            if ticks is None:
                code, desc = mt5.last_error()
                raise RuntimeError(f"MT5 error on copy_ticks_range: {code} {desc}")

            df_ticks = pd.DataFrame(ticks)
            if not df_ticks.empty:
                # Agrégation 1 minute pour cette heure
                ohlc_1min = ticks_to_ohlc_1min(df_ticks, tf,price_basis=price_basis,
                                               fill_missing=fill_missing)
                ohlc_1min.index = ohlc_1min.index
                # On ne garde que la fenêtre exacte de l'heure (utile si le resample déborde)
                mask = (ohlc_1min.index >= pd.Timestamp(hour_start)) & (ohlc_1min.index < pd.Timestamp(hour_end ))
                ohlc_1min = ohlc_1min.loc[mask]

                # ohlc_1min.index = ohlc_1min.index - pd.Timedelta(hours=1)
                if not ohlc_1min.empty:
                    count = insertPrices(symbol, TIME_RANGE, ohlc_1min)
                    print(f"Got {len(ticks)} ticks between {hour_start} and {hour_end} - {count} candles inserted.")
            # avancer d'une heure (pas besoin d'ajouter 1s: on traite les minutes, et la fenêtre suivante commence à hour_end)
            else:
                print("No candles inserted.")
            cur = hour_end


    finally:
        mt5.shutdown()

# --- Exemple d'utilisation ---
if __name__ == "__main__":
    copy_ticks_by_hour(
        symbol=market,
        start_utc=start_state,
        tf=TIME_RANGE_RESAMPLE,
        end_utc=end_date,
        price_basis="bid",      # "bid" | "ask" | "mid"
        fill_missing=False,     # True pour bougies plates sur minutes sans ticks
        flags=mt5.COPY_TICKS_ALL
    )


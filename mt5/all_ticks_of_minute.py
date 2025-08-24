import MetaTrader5 as mt5
import pandas as pd
from datetime import datetime, timedelta, timezone

try:
    # Python 3.9+ : zoneinfo standard
    from zoneinfo import ZoneInfo
    PARIS_TZ = ZoneInfo("Europe/Paris")
except Exception:
    # Au cas où (si zoneinfo indisponible), fallback en UTC direct
    PARIS_TZ = timezone.utc

# ========= Paramètres =========
symbol = "EURUSD"

# Minute cible : 11/08/2025 10:23 (heure de Paris)
minute_local = datetime(2025, 8, 11, 10, 23, 0, tzinfo=PARIS_TZ)
start_local = minute_local.replace(second=0, microsecond=0)
end_local   = start_local + timedelta(minutes=1)   # borne haute EXCLUSIVE

# Conversion en UTC (requis par MT5)
start_utc = start_local.astimezone(timezone.utc)
end_utc   = end_local.astimezone(timezone.utc)

# ========= Connexion MT5 =========
if not mt5.initialize():
    raise RuntimeError(f"MT5 init error: {mt5.last_error()}")

info = mt5.symbol_info(symbol)
if info is None:
    raise RuntimeError(f"Symbole introuvable: {symbol}")
if not info.visible:
    mt5.symbol_select(symbol, True)

# ========= Récupération des ticks =========
# COPY_TICKS_INFO => uniquement Bid/Ask (ce que tu veux)
ticks = mt5.copy_ticks_range(symbol, start_utc, end_utc, mt5.COPY_TICKS_INFO)

# ========= Mise en DataFrame =========
df = pd.DataFrame(ticks)

if df.empty:
    print(f"Aucun tick pour {symbol} entre {start_local} et {end_local} (heure de Paris).")
else:
    # Horodatage en ms (plus précis que 'time' en secondes)
    if "time_msc" in df.columns:
        df["time"] = pd.to_datetime(df["time_msc"], unit="ms", utc=True).dt.tz_convert(PARIS_TZ)
    else:
        df["time"] = pd.to_datetime(df["time"], unit="s", utc=True).dt.tz_convert(PARIS_TZ)

    # On ne garde que ce qui t'intéresse : time, bid, ask, volume(s)
    cols = [c for c in ["time","bid","ask","last","volume","volume_real","flags"] if c in df.columns]
    df = df[cols].sort_values("time").reset_index(drop=True)

    # Afficher TOUS les ticks (pas seulement tail)
    with pd.option_context("display.max_rows", None, "display.max_columns", None, "display.width", 200):
        print(df)

    # (Optionnel) sauvegarde CSV si tu veux l?ouvrir dans Excel
    out_path = f"{symbol}_ticks_{start_local.strftime('%Y%m%d_%H%M')}_paris.csv"
    df.to_csv(out_path, index=False, float_format="%.6f")
    print(f"\nEnregistré dans : {out_path}")
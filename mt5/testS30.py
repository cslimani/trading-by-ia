import MetaTrader5 as mt5
import pandas as pd
import numpy as np
from datetime import datetime, timedelta

symbol = "EURUSD"

if not mt5.initialize():
    raise RuntimeError(f"MT5 init error: {mt5.last_error()}")

to_time = datetime.now()
from_time = to_time - timedelta(minutes=10)

# Pour le FX, prends les updates Bid/Ask (INFO) ou ALL
ticks = mt5.copy_ticks_range(symbol, from_time, to_time, mt5.COPY_TICKS_ALL)
df = pd.DataFrame(ticks)
df['time'] = pd.to_datetime(df['time'], unit='s')
df = df.set_index('time')

# ===== Choix du prix =====
use_price = "ask"   # "bid" | "ask" | "mid"
if use_price == "mid":
    price = df[['bid','ask']].mean(axis=1).dropna()
elif use_price == "ask":
    price = df['ask'].dropna()
else:
    price = df['bid'].dropna()

# ===== OHLC en 30 secondes =====
# label/closed "right" pour fermer la bougie à xx:00, xx:30, xx:60, etc.
ohlc_30s = price.resample('1min', label='right', closed='right').ohlc()

# ===== Volumes =====
# Sur le FX, il n?y a souvent pas de volume réel -> on utilise le "tick volume" (nb de ticks)
tick_volume = df['bid'].resample('1min').count()  # ou compter price.index
ohlc_30s['tick_volume'] = tick_volume.reindex(ohlc_30s.index).fillna(0).astype(int)

# Si 'volume_real' existe et est non nul pour ton symbole, tu peux aussi sommer :
if 'volume_real' in df.columns:
    ohlc_30s['volume_real'] = df['volume_real'].resample('1min').sum().reindex(ohlc_30s.index).fillna(0)

print(ohlc_30s.tail())
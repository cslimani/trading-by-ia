import MetaTrader5 as mt5
import pandas as pd
from datetime import datetime, timedelta
from time import sleep

from dateutil.tz import tz

from db_utils import insertPrices

symbol = "EURUSD"
only_closed = True
# timeframe = mt5.TIMEFRAME_M1
timeframe = mt5.TIMEFRAME_M5
# timeframe = mt5.TIMEFRAME_M15
# timeframe = mt5.TIMEFRAME_H1
# timeframe = mt5.TIMEFRAME_H4
date_limit = datetime(2024, 1, 1)
batch_size = 1000

if timeframe == mt5.TIMEFRAME_M1:
    TIME_RANGE = "M1"
if timeframe == mt5.TIMEFRAME_M5:
    TIME_RANGE = "M5"
if timeframe == mt5.TIMEFRAME_M15:
    TIME_RANGE = "M15"
if timeframe == mt5.TIMEFRAME_H1:
    TIME_RANGE = "H1"
if timeframe == mt5.TIMEFRAME_H4:
    TIME_RANGE = "H4"

if not mt5.initialize():
    raise RuntimeError(f"MT5 init error: {mt5.last_error()}")

info = mt5.symbol_info(symbol)
if info is None:
    raise RuntimeError(f"Symbole introuvable: {symbol}")
if not info.visible:
    mt5.symbol_select(symbol, True)

pos = 0
while True:
    print(f"Loop pos={pos}  symbol={symbol} batch_size={batch_size}")
    rates = mt5.copy_rates_from_pos(symbol, timeframe, pos, batch_size)
    if rates is None or len(rates) == 0:
        break  # plus de données

    df = pd.DataFrame(rates)
    df["time"] = pd.to_datetime(df["time"], unit="s", utc=True)

    if only_closed:
        # une bougie M1 est considérée close si now >= last_time + 1 minute
        now = datetime.now(tz=tz.tzutc())
        last_time = df.iloc[-1]["time"].to_pydatetime()
        if now < last_time + timedelta(minutes=1):
            df = df.iloc[:-1]

    df.sort_values("time", inplace=True)
    df = df[["time", "open", "high", "low", "close", "tick_volume", "spread", "real_volume"]]

    # df["time"] = pd.to_datetime(df["time"], unit="s")
    df = df.set_index("time").sort_index()
    insertPrices(symbol, TIME_RANGE, df)

    # Vérifie si la plus ancienne bougie de ce batch est avant date_limit
    if df.index[-1] <= pd.Timestamp(date_limit).tz_localize("Europe/Paris"):
        break
    pos += batch_size





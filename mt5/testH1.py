import MetaTrader5 as mt5
import pandas as pd
from datetime import datetime, timedelta

# 1. Connexion à MetaTrader 5
if not mt5.initialize():
    print("Échec de l'initialisation de MetaTrader5")
    mt5.shutdown()
    quit()

symbol = "XAUUSD"  # adapte selon ton broker
nb_candles = 100
timeframe_hours = 1

end_time = datetime(2023, 1, 10, 0, 0)
start_time = end_time - timedelta(hours=nb_candles * timeframe_hours + 1)

ticks = mt5.copy_ticks_range(symbol, start_time, end_time, mt5.COPY_TICKS_ALL)

if ticks is None or len(ticks) == 0:
    print(f"Aucun tick trouvé pour {symbol}")
    mt5.shutdown()
    quit()

# 4. Convertir en DataFrame
df = pd.DataFrame(ticks)
df.sort_values("time", inplace=True)
df['time'] = pd.to_datetime(df['time'], unit='s')

# 5. Grouper par heure et récupérer le dernier Bid et Ask de chaque bougie
df['hour'] = df['time'].dt.floor('h')
candles = df.groupby('hour').agg({'bid': 'last', 'ask': 'last'}).reset_index()

# 6. Garder seulement les 10 dernières
candles = candles.tail(nb_candles)

# 7. Afficher
print(f"\nDerniers {nb_candles} closings Bid/Ask H1 sur {symbol} :")
print(candles.to_string(index=False))

# 8. Fermer MT5
mt5.shutdown()

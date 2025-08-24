import json
from datetime import datetime, timezone

import pandas as pd
from pandas import DataFrame

import psycopg2

DB_CONFIG = {
    "host": "192.168.0.113",
    "port": 5432,
    "dbname": "trading",
    "user": "trading",
    "password": "trading"
}


def getPrices(market_label, time_range, start_date, end_date, return_text = True):
    global cur, conn
    query = """
    SELECT 
    date,
    open,
    high,
    low,
    close,
    volume
    FROM candle
    WHERE market = %s
      AND time_range = %s
      AND date BETWEEN %s AND %s
    ORDER BY date;
    """
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        cur = conn.cursor()

        cur.execute(query, (market_label, time_range, start_date, end_date))
        rows = cur.fetchall()

        colnames = [desc[0] for desc in cur.description]

        if return_text :
            price_list = ",".join(colnames) + "\n"
            for row in rows:
                price_list += ",".join(str(val) for val in row) + "\n"
            return price_list
        else :
            prices = [
                {
                    "date": row["date"],
                    "open": float(row["open"]),
                    "high": float(row["high"]),
                    "low": float(row["low"]),
                    "close": float(row["close"]),
                    "volume": float(row["volume"]) if row["volume"] is not None else 0.0
                }
                for row in [dict(zip(colnames, r)) for r in rows]
            ]
            return prices
    except Exception as e:
        print("Erreur :", e)

    finally:
        if 'cur' in locals():
            cur.close()
        if 'conn' in locals():
            conn.close()

def insertPrices(market, time_range, prices : DataFrame):
    global cursor, conn
    conn = psycopg2.connect(**DB_CONFIG)
    cursor = conn.cursor()
    count = 0
    try:
        for index, price in prices.iterrows():
            date = (
                index.tz_convert("Etc/GMT+1").tz_localize(None)
                if hasattr(index, "tz_convert")
                else index
            )

            cursor.execute(
                """
                SELECT 1 FROM candle
                WHERE market = %s AND time_range = %s AND date = %s
                LIMIT 1
                """,
                (market, time_range, date)
            )
            if cursor.fetchone():
                continue
            if float(price['open']) == 0 :
                print("Erreur :", price)

            cursor.execute(
                """
                INSERT INTO candle (market, market_label, open, high, low, close, time_range, date, volume)
                VALUES (%s,%s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    market,
                    market,
                    round(float(price['open']), 5),
                    round(float(price['high']), 5),
                    round(float(price['low']), 5),
                    round(float(price['close']), 5),
                    time_range,
                    date,
                    round(float(price['tick_volume']), 5),
                )
            )
            count += 1
        conn.commit()
        return count
    except Exception as e:
        print("Erreur :", e)
    finally:
        cursor.close()
        conn.close()

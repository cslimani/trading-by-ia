package com.trading.indicator;

import java.util.List;

import com.trading.entity.Candle;

public class EmaCalculator {

	public static void computeEMA(List<Candle> candles, int period) {
		if (candles == null || candles.size() < period) {
			return;
		}

		final double k = 2.0 / (period + 1);

		// 1) EMA initiale = SMA(20) sur les 20 premières bougies
		double sma = 0.0;
		for (int i = 0; i < period; i++) {
			sma += candles.get(i).close;
		}
		sma /= period;
		candles.get(period - 1).ema = sma;
		candles.get(period - 1).emaSlope = null; // pas de pente au premier point EMA

		// 2) EMA récursive + pente (delta d'EMA)
		for (int i = period; i < candles.size(); i++) {
			Candle prev = candles.get(i - 1);
			Candle cur  = candles.get(i);

			double emaPrev = prev.ema;
			double ema = (cur.close * k) + (emaPrev * (1 - k));
			cur.ema = ema;

			// pente = EMA_t - EMA_{t-1}
			cur.emaSlope = ema - emaPrev;
		}

	}

}

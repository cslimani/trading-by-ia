package com.trading.indicator;

import java.util.List;

import com.trading.entity.Candle;

public class RsiCalculator {

	public static void computeRSI(List<Candle> candles, int period) {
		double gainSum = 0.0;
		double lossSum = 0.0;

		// Moyennes initiales sur 14 premières variations
		for (int i = 1; i <= period; i++) {
			double change = candles.get(i).close - candles.get(i - 1).close;
			if (change >= 0) {
				gainSum += change;
			} else {
				lossSum += -change;
			}
		}
		double avgGain = gainSum / period;
		double avgLoss = lossSum / period;

		// RSI pour la bougie n°14
		double rs = avgLoss == 0 ? 0 : avgGain / avgLoss;
		candles.get(period).rsi = avgLoss == 0 ? 100.0 : (100 - (100 / (1 + rs)));

		// RSI récursif
		for (int i = period + 1; i < candles.size(); i++) {
			double change = candles.get(i).close - candles.get(i - 1).close;
			double gain = Math.max(change, 0);
			double loss = Math.max(-change, 0);

			// Moyennes lissées de Wilder
			avgGain = ((avgGain * (period - 1)) + gain) / period;
			avgLoss = ((avgLoss * (period - 1)) + loss) / period;

			rs = avgLoss == 0 ? 0 : avgGain / avgLoss;
			double rsi = avgLoss == 0 ? 100.0 : (100 - (100 / (1 + rs)));
			candles.get(i).rsi = rsi;
			if (Double.isNaN(rsi)) {
				throw new RuntimeException("RSI is NAN");
			}
		}

	}

}

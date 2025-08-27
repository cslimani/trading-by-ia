package com.trading.indicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.trading.entity.Candle;

import lombok.Getter;

@Getter
public class EmaCalculator {

	private Map<Integer, Double> mapEma = new HashMap<Integer, Double>();
	private Map<Integer, Double> mapslope = new HashMap<Integer, Double>();

	public EmaCalculator(List<Candle> candles, int period) {
		if (candles == null || candles.size() < period) {
			return;
		}

		final double k = 2.0 / (period + 1);

		// 1) EMA initiale = SMA(20) sur les 20 premières bougies
		double sma = 0.0;
		for (int i = 0; i < period; i++) {
			sma += candles.get(i).getClose();
		}
		sma /= period;
		mapEma.put(period - 1, sma);

		// 2) EMA récursive + pente (delta d'EMA)
		for (int i = period; i < candles.size(); i++) {
			Candle cur  = candles.get(i);

			double emaPrev = mapEma.get(i - 1);
			double ema = (cur.getClose() * k) + (emaPrev * (1 - k));
			mapEma.put(i, ema);
			// pente = EMA_t - EMA_{t-1}
			mapslope.put(i, ema - emaPrev);
		}
	}

	public Double get(Integer index) {
		return mapEma.get(index);
	}

	public Double getSlope(Integer index) {
		return mapEma.get(index);
	}
}

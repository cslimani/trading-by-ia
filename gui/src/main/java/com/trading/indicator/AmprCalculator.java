package com.trading.indicator;

import java.util.List;
import java.util.Map;

import com.trading.entity.Candle;

public class AmprCalculator {

	public static Double getAMPR(List<Candle> candles, Map<Integer, Double> mapATR,
			Map<Integer, Double> mapEMA, int indexStart, int indexEnd) {
		Double sum = 0d;
		for (int i= indexStart; i < indexEnd + 1; i++) {
			Candle c = candles.get(i);
			if (c.isDate(2025, 1, 2, 7, 32, 30)) {
				System.out.println();
			}
			sum += Math.abs(candles.get(i).getClose() - mapEMA.get(i));
		}
		Double emaAverage = sum / (indexEnd + 1 - indexStart);
		Double ampr = emaAverage / mapATR.get(indexEnd);
		return ampr;
	}

	public static Double getConsistency(Map<Integer, Double> mapEMA, int indexStart, int indexEnd) {
		Double emaSlopeAverageAbs = getEmaSlopeAverage(mapEMA, indexStart, indexEnd, true);
		Double emaSlopeAverage = getEmaSlopeAverage(mapEMA, indexStart, indexEnd, false);
		Double consistency = Math.abs(emaSlopeAverage) / emaSlopeAverageAbs;
		return consistency;
	}

	private static Double getEmaSlopeAverage(Map<Integer, Double> mapEMA, int indexStart, int indexEnd, boolean abs) {
		Double sum = 0d;
		for (int i= indexStart; i < indexEnd + 1; i++) {
			if (abs) {
				sum += Math.abs(mapEMA.get(i));
			} else {
				sum += mapEMA.get(i);
			}
		}
		return sum / (indexEnd + 1 - indexStart);
	}
}

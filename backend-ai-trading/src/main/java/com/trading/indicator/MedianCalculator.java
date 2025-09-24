package com.trading.indicator;

import java.util.List;
import java.util.stream.Collectors;

import com.trading.entity.Candle;

public class MedianCalculator {

	public static double quantile(List<Candle> candles, double quantile) {
		if (candles == null || candles.isEmpty()) {
			throw new IllegalArgumentException("Liste de candles vide !");
		}

		List<Double> closes = candles.stream()
				 .map(c -> (c.high + c.low + c.close) / 3.0)
				.sorted()
				.collect(Collectors.toList());

		return closes.get(Double.valueOf(closes.size()*quantile).intValue());
	}
	
	public static double median(List<Candle> candles) {
		return quantile(candles, 0.5d);
	}
	
	public static double medianPrices(List<Double> prices) {
		if (prices == null || prices.isEmpty()) {
			throw new IllegalArgumentException("Empty list");
		}

		List<Double> closes = prices.stream()
				.sorted()
				.collect(Collectors.toList());

		return closes.get(closes.size() / 2);
	}
}

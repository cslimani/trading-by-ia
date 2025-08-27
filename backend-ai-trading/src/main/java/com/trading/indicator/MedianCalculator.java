package com.trading.indicator;

import java.util.List;
import java.util.stream.Collectors;

import com.trading.entity.Candle;

public class MedianCalculator {

	public static double median(List<Candle> candles) {
		if (candles == null || candles.isEmpty()) {
			throw new IllegalArgumentException("Liste de candles vide !");
		}

		List<Double> closes = candles.stream()
				 .map(c -> (c.high + c.low + c.close) / 3.0)
				.sorted()
				.collect(Collectors.toList());

		return closes.get(closes.size() / 2);
	}
}

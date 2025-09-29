package com.trading.indicator;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.data.domain.PageRequest;

import com.trading.entity.Candle;
import com.trading.repository.CandleRepository;

import lombok.Getter;

@Getter
public class EmaCalculator {

	private Map<Integer, Double> mapEma = new HashMap<>();
	private Map<LocalDateTime, Double> mapDateEma = new HashMap<>();
	private Map<Integer, Double> mapslope = new HashMap<>();

	public EmaCalculator(List<Candle> candles, int period) {
		init(candles, period);
	}

	private void init(List<Candle> candles, int period) {
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
		mapDateEma.put(candles.get(period).getDate(), sma);
		
		// 2) EMA récursive + pente (delta d'EMA)
		for (int i = period; i < candles.size(); i++) {
			Candle cur  = candles.get(i);

			double emaPrev = mapEma.get(i - 1);
			double ema = (cur.getClose() * k) + (emaPrev * (1 - k));
			mapEma.put(i, ema);
			mapDateEma.put(candles.get(i).getDate(), ema);
			// pente = EMA_t - EMA_{t-1}
			mapslope.put(i, ema - emaPrev);
		}
		
	}

	public EmaCalculator(CandleRepository candleRepository, Candle last, int period) {
		List<Candle> candles = candleRepository.findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(last.getMarket(), last.getTimeRange(),
				last.getDate(), PageRequest.of(0, 3*period));
		Collections.reverse(candles);
		IntStream.range(0, candles.size()).forEach(p -> candles.get(p).setIndex(p));
		init(candles, period);
	}

	public Double get(Integer index) {
		return mapEma.get(index);
	}
	
	public Double get(LocalDateTime date) {
		return mapDateEma.get(date);
	}

	public Double getSlope(Integer index) {
		return mapEma.get(index);
	}
}

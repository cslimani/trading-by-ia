package com.trading.service;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.trading.entity.Candle;
import com.trading.enums.EnumTimeRange;
import com.trading.indicator.extremum.CandleMinMaxAnalyzerMine;
import com.trading.indicator.extremum.SwingExtremaFinder;

//@Component
public class MixMaxChecker extends AbstractService implements CommandLineRunner {

	LocalDateTime startDate = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);
	LocalDateTime endDate = LocalDateTime.of(2026, Month.DECEMBER, 1, 0, 0);
	String market = "US100";
	EnumTimeRange timeRange = EnumTimeRange.M5;

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Impulse Detection starting");

		List<Candle> candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(
				market,
				timeRange,
				startDate,
				endDate);
//		CandleMinMaxAnalyzerMine analyzer = new CandleMinMaxAnalyzerMine(8, candles);
//		analyzer.process();
		SwingExtremaFinder analyzer = new SwingExtremaFinder();
		analyzer.findExtrema(candles, 8);
	}

}

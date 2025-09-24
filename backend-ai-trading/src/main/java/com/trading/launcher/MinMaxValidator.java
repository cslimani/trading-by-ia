package com.trading.launcher;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.trading.entity.Candle;
import com.trading.enums.EnumTimeRange;
import com.trading.indicator.AtrCalculator;
import com.trading.indicator.extremum.Extremum;
import com.trading.indicator.extremum.HybridMinMaxAnalyzer;
import com.trading.repository.CandleRepository;
import com.trading.service.AbstractService;

import lombok.extern.slf4j.Slf4j;

@Component
@Profile("min-max-validator")
@Slf4j
public class MinMaxValidator extends AbstractService implements CommandLineRunner {


	private static final double MIN_MAX_ATR_RATIO = 2;
	@Autowired
	CandleRepository candleRepository;

	@Override
	public void run(String... args) throws Exception {
		List<Candle> candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(
				"GOLD"
				,EnumTimeRange.M5
				, LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)
				,LocalDateTime.of(2020, Month.FEBRUARY, 1, 0, 0));
		setIndex(candles);
		AtrCalculator.compute(candles, 50);
//		SimpleMinMaxAnalyzer minMaxAnalyzer = new SimpleMinMaxAnalyzer(MIN_MAX_ATR_RATIO, 7);
		HybridMinMaxAnalyzer minMaxAnalyzer = new HybridMinMaxAnalyzer(MIN_MAX_ATR_RATIO, 7);
		List<Extremum> extremums = null;
		for (int i = 0; i < candles.size(); i++) {
			Candle c = candles.get(i);
			extremums = minMaxAnalyzer.process(c);
			//			break;
		}
		//		;
		List<String> computedList = extremums.stream().map(e -> e.getDate()).map(d -> d.toString()).toList();
		//		computedList.forEach(d -> System.out.println(d));
		List<String> goodValuesList = IOUtils.readLines(new ClassPathResource("min-max-list.txt").getInputStream(), "UTF-8");
		//		Assert.isTrue(computedList.equals(goodValuesList), "List are not equals");
		for (int i = 0; i < goodValuesList.size(); i++) {
			if (!goodValuesList.get(i).equals(computedList.get(i))) {
				System.out.println("Wrong value at index " + i + " - good value is " + goodValuesList.get(i) +
						" - computed value is " + computedList.get(i));
				break;
			}
		}

	}

}

package com.trading.launcher;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.trading.entity.Candle;
import com.trading.entity.HotSpot;
import com.trading.enums.EnumTimeRange;
import com.trading.enums.ExtremumType;
import com.trading.indicator.AtrCalculator;
import com.trading.indicator.EmaCalculator;
import com.trading.indicator.MacdCalculator;
import com.trading.indicator.RsiCalculator;
import com.trading.indicator.extremum.Extremum;
import com.trading.indicator.extremum.SwingExtremaFinder;
import com.trading.service.AbstractService;

@Component
@Profile("accumulation-ltf")
public class AccumulationLTF extends AbstractService implements CommandLineRunner {

	private static final int EXTREMUM_PARAM = 5;
	private static final double ALPHA_BREAK = 0.3;
	private static final double TETA_BREAK = 0.3;

	SwingExtremaFinder analyzer = new SwingExtremaFinder();

	LocalDateTime startDate = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);
	LocalDateTime endDate = LocalDateTime.of(2026, Month.DECEMBER, 1, 0, 0);
	String market = "US100";
	EnumTimeRange timeRange = EnumTimeRange.M1;

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Impulse Detection starting");
		hotSpotRepository.deleteAll();
		List<HotSpot> hotspotsHTF = hotSpotRepository.findByCode("FULL_HTF");
		List<Candle> candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(
				market,
				timeRange,
				startDate,
				endDate);
		setIndex(candles);
		computeHigherLowRunsNaive(candles);
		EmaCalculator.computeEMA(candles, 20);
		RsiCalculator.computeRSI(candles, 14);
		MacdCalculator.computeMacd(candles);
		AtrCalculator.compute(candles, 10);
		List<Extremum> extremums = analyzer.findExtrema(candles, 7, false);
		for (int i = 100; i < candles.size() - 100; i++) {
			Candle c = candles.get(i);
			if (c.getDate().getHour() < 6 || c.getDate().getHour() > 21) {
				continue;
			}
			if (c.getPriorHigherLowRun() < 50) {
				continue;
			}
			if(c.getPosteriorHigherLowRun() < 5) {
				continue;
			}
			if(c.getRsi() > 25) {
				continue;
			}

			if (!isMACDIncreasing(c, candles, 10)) {
				continue;
			}

			//			Candle impulsionStart = getImpulsionStart(c, candles);
//			Candle impulsionStart = getFirstExtremumBefore(c, extremums, Type.MAX);
			Candle impulsionEnd = c;

//			if (!isFreeSpaceBehind(impulsionStart, impulsionEnd, candles, 60, 0.5)) {
				//				increaseCount("NO_FREE_SPACE_BEHIND");
				//				continue;
//			}

//			if (isInHTFZone(hotspotsHTF, impulsionEnd)) {
//				increaseCount("IS_IN_HTF_ZONE");
//			}
			//ACCUMULATION MAY START
//			lookForTrade(impulsionStart, impulsionEnd, candles);
//			Candle candleEnd = candleRepository.findNthCandleAfterDate(market,timeRange.name(), c.getDate(), 50);
			//			saveHotSpot(impulsionStart.getDate(),
			//					candleEnd.getDate(),
			//					List.of(impulsionStart.getDate(), c.getDate()));
			//			System.out.println(format(impulsionStart.getDate()) + " " + format(candleEnd.getDate()));
		}
		displayMapCount();
	}


	private void saveHotSpot(LocalDateTime dateStart, LocalDateTime dateEnd, List<LocalDateTime> keyDates) {
		hotSpotRepository.save(HotSpot.builder()
				.market(market)
				.timeRange(timeRange)
				.dateStart(dateStart)
				.dateEnd(dateEnd)
				.keyDates(keyDates)
				.code("ACCUMULATION_LTF")
				.creationDate(LocalDateTime.now())
				.build());
	}


	private void lookForTrade(Candle impulsionStart, Candle accumulationStart, List<Candle> candles) {
		double lowAccumulationPrice = accumulationStart.getLow();
		double topImpulsion = impulsionStart.getHigh();
		double alphaBreak = ALPHA_BREAK;
		double tetaBreak  = TETA_BREAK;
		boolean isSpring = false;
		List<Extremum> extremumsInit = analyzer.findExtrema(candles, EXTREMUM_PARAM, false);
		for (int i = accumulationStart.getIndex(); i < candles.size() - 100; i++) {
			Candle c = candles.get(i);
//			if (c.isDate(19, 6) && c.isTime(15, 16, 0)) {
//				getClass();
//			}
			List<Extremum> extremums = extremumsInit.stream()
					.filter(e -> e.getCandle().getIndex() <= c.getIndex() - e.getK())
					.filter(e -> e.getCandle().getIndex() >= accumulationStart.getIndex())
					.sorted(Comparator.comparingInt((Extremum e)  -> e.getCandle().getIndex()).reversed())
					.toList();
			// prices goes too high
			double maxPriceAllowed = lowAccumulationPrice + (topImpulsion - lowAccumulationPrice)*0.75;
			if (c.getHigh() >= maxPriceAllowed) {
				break;
			}

			// prices drops too much
			if (c.getOpen() < lowAccumulationPrice && c.getClose() < lowAccumulationPrice) {
				break;
			}


			if (c.getLow() < lowAccumulationPrice) {
				isSpring = true;
			}

			//BOS
			if (isBOSAndCHOCH(c, candles, extremums, tetaBreak, alphaBreak) && isSpring) {
				if (c.getDate().getHour() < 9 || c.getDate().getHour() > 21) {
					increaseCount("NOT_RIGHT_TIME");
					break;
				}
				if (isSpring) {
					increaseCount("WITH_SPRING");
				}
				increaseCount("TRADE_ENTRY");
				saveHotSpot(impulsionStart.getDate(),
						c.getDate(),
						List.of(impulsionStart.getDate(), accumulationStart.getDate()));
				break;
			}
			processBOS(c, extremums, candles, alphaBreak);
		}
	}


	private boolean isBOSAndCHOCH(Candle c,  List<Candle> candles, List<Extremum> extremums, double tetaBreak, double alphaBreak) {
		List<Extremum> maximums = extremums.stream().filter(Extremum::isMax).toList();
		if (maximums.size() < 2) {
			return false;
		}

		// BOS
		boolean bos = extremums.stream()
		.filter(e -> e.getType() == ExtremumType.MAX)
		.anyMatch(e -> 	e.getBosBreakingCandle() == null 
					&& c.getClose() > e.getCandle().getHigh() + alphaBreak*c.getAtr());
		if (!bos) {
			return false;
		}

//		// CHOCH
//		Candle lastMax = maximums.get(0).getCandle();
//		Candle lastMinBeforeMax = getFirstExtremumBefore(lastMax, extremums, Type.MIN);
//
//		for (int i = lastMax.getIndex(); i < c.getIndex(); i++) {
//			Candle cTmp = candles.get(i);
//			if (cTmp.getLow() < lastMinBeforeMax.getLow() + tetaBreak*cTmp.getAtr()) {
//				return false;
//			}
//		}
		return true;
	}


	private boolean isCHOCH(Candle c, List<Extremum> extremums, List<Candle> candles, double tetaBreak) {
		List<Extremum> minimums = extremums.stream().filter(Extremum::isMin).toList();
		if (minimums.size() < 2) {
			return false;
		}
		Candle lastMin = minimums.get(0).getCandle();
		Candle beforelastMin = minimums.get(1).getCandle();
		if (lastMin.getLow() < beforelastMin.getLow() + tetaBreak*lastMin.getAtr()) {
			return false;
		}
		return true;
	}


	private boolean isBOS(Candle c, List<Extremum> extremums) {
		return extremums.stream()
				.anyMatch(e -> e.getBosBreakingCandle() != null
				&& e.getBosBreakingCandle().getIndex() == c.getIndex());				
	}


	private void processBOS(Candle c, List<Extremum> extremums, List<Candle> candles, double alphaBreak) {
		extremums.stream()
		.filter(e -> e.getType() == ExtremumType.MAX)
		.forEach(e -> {
			if (e.getBosBreakingCandle() == null 
					&& c.getClose() > e.getCandle().getHigh() + alphaBreak*c.getAtr()) {
				e.setBosBreakingCandle(c);
			}
		});
	}


	private boolean isInHTFZone(List<HotSpot> hotspotsHTF, Candle accumulationStart) {
		return hotspotsHTF.stream()
				.anyMatch(c -> {
					LocalDateTime impulsionTopDate = c.getKeyDates().get(1);
					LocalDateTime zoneBreakDate = c.getKeyDates().get(2);
					LocalDateTime accumulationStartD = accumulationStart.getDate();
					return accumulationStartD.isAfter(impulsionTopDate) && accumulationStartD.isBefore(zoneBreakDate);
				});
	}


	private boolean isFreeSpaceBehind(Candle impulsionStart, Candle impulsionEnd, List<Candle> candles, int count, double ratio) {
		double priceThresold = impulsionEnd.getLow() + (impulsionStart.getOpen() - impulsionEnd.getLow())*ratio;
		Integer indexStart = impulsionStart.getIndex();
		for (int i = 0; i <= count; i++) {
			Candle c = candles.get(indexStart-i);
			if (c.getLow() < priceThresold) {
				return false;
			}
		}
		return true;
	}

	private Candle getImpulsionStart(Candle c, List<Candle> candles) {
		for (int i = c.getIndex(); i >= 0; i--) {
			c = candles.get(i);
			if (c.getMacdDiff() > 0) {
				break;
			}
		}
		return c;
	}

	private boolean isMACDIncreasing(Candle c, List<Candle> candles, int count) {
		for (int i = c.getIndex() + 1; i <= count; i++) {
			Candle tmpCandle = candles.get(i);
			Candle tmpCandlePlus1 = candles.get(i+1);
			if (tmpCandle.getMacdDiff() < 0 &&
					tmpCandlePlus1.getMacdDiff() < 	tmpCandle.getMacdDiff()) {
				return false;
			}
		}
		return true;
	}

	public String format(LocalDateTime dateTime) {
		return dateTime.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
	}

	public void computeHigherLowRunsNaive(List<Candle> candles) {
		int n = candles.size();
		for (int i = 0; i < n; i++) {
			double ref = candles.get(i).low;

			int prior = 0;
			for (int k = i - 1; k >= 0; k--) {
				if (candles.get(k).low > ref) prior++;
				else break;
			}

			int posterior = 0;
			for (int k = i + 1; k < n; k++) {
				if (candles.get(k).low > ref) posterior++;
				else break;
			}

			candles.get(i).priorHigherLowRun = prior;
			candles.get(i).posteriorHigherLowRun = posterior;
		}
	}

}

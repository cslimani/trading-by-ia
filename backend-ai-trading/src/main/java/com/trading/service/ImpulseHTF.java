package com.trading.service;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.trading.entity.Candle;
import com.trading.entity.HotSpot;
import com.trading.enums.EnumTimeRange;
import com.trading.indicator.EmaCalculator;
import com.trading.indicator.MacdCalculator;
import com.trading.indicator.RsiCalculator;
import com.trading.indicator.extremum.Extremum;
import com.trading.indicator.extremum.SwingExtremaFinder;
import com.trading.indicator.extremum.SwingExtremaFinder.Type;

@Component
@Profile("impulse-htf")
public class ImpulseHTF extends AbstractService implements CommandLineRunner {

	LocalDateTime fetchStartDate = LocalDateTime.of(2024, Month.JANUARY, 1, 0, 0);
	LocalDateTime startDate = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);
	LocalDateTime endDate = LocalDateTime.of(2026, Month.DECEMBER, 1, 0, 0);
	String market = "US100.cash";

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Full Strategy Detection starting");
		hotSpotRepository.deleteAll();
		SwingExtremaFinder analyzer = new SwingExtremaFinder();
		Set<LocalDateTime> startImpulseAlreadyTaken = new HashSet<>();
		List<Candle> candlesH4 = buildValidPrices(EnumTimeRange.H4);

		List<Candle> candlesH1 = buildValidPrices(EnumTimeRange.H1);

		EnumTimeRange workTimeframe = EnumTimeRange.H1;
		List<Candle> candlesM15 = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(
				market,
				workTimeframe,
				fetchStartDate,
				endDate);
		setIndex(candlesM15);
		EmaCalculator.computeEMA(candlesM15, 200);
		RsiCalculator.computeRSI(candlesM15, 14);
		MacdCalculator.computeMacd(candlesM15);

		List<Extremum> tops = analyzer.findExtrema(candlesM15, 5)
				.stream()
				.filter(e -> e.type == Type.MAX)
				.toList();
		List<Extremum> bottoms = analyzer.findExtrema(candlesM15, 8, false)
				.stream()
				.filter(e -> e.type == Type.MIN)
				.toList();
		processBOS(tops, candlesM15);

		int nbSuccess = 0;
		for (int i = 0; i < candlesM15.size(); i++) {
			Candle c = candlesM15.get(i);
			if (c.getDate().isBefore(startDate)) {
				continue;
			}
			if (c.getRsi() < 60) {
				continue;
			}
			if (c.getEmaSlope() < 0) {
				continue;
			}
			if (c.getClose() < c.getEma()) {
				continue;
			}
			if (!isInside(c, candlesH4)) {
//				continue;
			}
			if (!isInside(c, candlesH1)) {
				continue;
			}

			if (!isLocalMaxmimum(c, candlesM15, 5)) {
				continue;
			}
			Candle impulseStart = getImpulseStart(c, candlesM15, bottoms);
			Candle impulseEnd = c;
			if (startImpulseAlreadyTaken.contains(impulseStart.getDate())) {
				continue;
			}
			startImpulseAlreadyTaken.add(impulseStart.getDate());
			if (c.getIndex() -  impulseStart.getIndex() <= 4) {
				continue;
			}
			
			int nbBOS = 0;
			Extremum firstBOS = null;
			for (int p = 0; p < tops.size(); p++) {
				Extremum top = tops.get(p);
				Candle breakingCandle = top.bosBreakingCandle;
				if (breakingCandle != null
						&& breakingCandle.getDate().isAfter(impulseStart.getDate())
						&& !breakingCandle.getDate().isAfter(c.getDate())) {
					nbBOS++;
					if (firstBOS == null) {
						firstBOS = top;
					}
				}
			}
			if (nbBOS == 0) {
//				continue;
			}
			nbSuccess++;
			Candle zoneBreaker = getZoneBreaker(impulseStart, impulseEnd, candlesM15);
			
			System.out.println(c.getDate() +  " - OK M15 - with " + nbBOS + " BOS");
			Candle candleEnd = candleRepository.findNthCandleAfterDate(market, workTimeframe.name(), c.getDate(), 30);
			hotSpotRepository.save(HotSpot.builder()
					.market(market)
					.timeRange(workTimeframe)
					.dateStart(impulseStart.getDate())
					.keyDates(List.of(impulseStart.getDate(), c.getDate(), zoneBreaker.getDate()))
					.dateEnd(zoneBreaker.getDate())
					.code("FULL_HTF")
					.creationDate(LocalDateTime.now())
					.build());
			//			Optional<Extremum> maxmimumOpt = findMaximum(c, extremums);
			//			if (maxmimumOpt.isPresent()) {
			//				Extremum validExtremum = maxmimumOpt.get();
			//				if (!validExtremums.contains(validExtremum)) {
			//					nbSuccess++;
			//					System.out.println(c.getDate() +  " - OK M15 with max at " + validExtremum.candle.getDate());
			//					validExtremums.add(validExtremum);
			//					hotSpotRepository.save(HotSpot.builder()
			//							.market(market)
			//							.timeRange(EnumTimeRange.M15)
			//							.dateStart(validExtremum.candle.getDate().plusHours(-10))
			//							.dateEnd(validExtremum.candle.getDate())
			//							.code("FULL_HTF")
			//							.creationDate(LocalDateTime.now())
			//							.build());
			//				}
			//			} else {
			//				//				System.out.println(c.getDate() +  " - OK M15 - MAXIMUM NOT FOUND for " + c.getDate());
			//			}
		}
		System.out.println("Total success : " + nbSuccess);
		System.out.println("Leave from top  : " + getCount("LEAVE_FROM_TOP"));
	}

	private Candle getZoneBreaker(Candle impulseStart, Candle impulseEnd, List<Candle> candles) {
		double zoneLow = impulseStart.getLow();
		double zoneHigh = impulseEnd.getHigh();
		for (int i = 0; i < candles.size()-1; i++) {
			Candle c = candles.get(i);
			if (c.getDate().isBefore(impulseEnd.getDate())) {
				continue;
			}
			if (c.getOpen() > zoneHigh && c.getClose() > zoneHigh) {
				increaseCount("LEAVE_FROM_TOP");
				return c;
			}
			if (c.getClose() < zoneLow) {
				return c;
			}
		}
		return null;
	}

	private Candle getImpulseStart(Candle candle, List<Candle> candlesM15, List<Extremum> bottoms) {
		Candle impulseStart = getImpulseStartMACD(candle, candlesM15);
		Candle impulseStartMinimum = getImpulseStartMinimum(candle, candlesM15, bottoms);
		return List.of(impulseStart, impulseStartMinimum).stream()
				.min(Comparator.comparingInt(c -> c.getIndex()))
				.get();
	}

	private Candle getImpulseStartMinimum(Candle c, List<Candle> candles, List<Extremum> bottoms) {
		return bottoms.stream()
		.filter(e -> e.getDate().isBefore(c.getDate()))
		.max(Comparator.comparingInt(e -> e.getCandle().getIndex()))
		.get()
		.getCandle();
	}

	private void processBOS(List<Extremum> tops, List<Candle> candles) {
		for (int p = 0; p < tops.size(); p++) {
			Extremum e = tops.get(p);
			if (!e.date.isAfter(startDate)) {
				continue;
			}
			if (e.bosBreakingCandle != null) {
				continue;
			}
			for (int i = 0; i < candles.size(); i++) {
				Candle c = candles.get(i);
				if (!c.getDate().isAfter(e.date)) {
					continue;
				}
				if (c.getOpen() > e.candle.getHigh() && c.getClose() > e.candle.getHigh()) {
					e.bosBreakingCandle = c;
					//					System.out.println("High " + e.getDate() + " broken by BOS " + c.getDate());
					break;
				}
			}
		}
	}

	private boolean isLocalMaxmimum(Candle c, List<Candle> candles, int nbCandles) {
		for (int i=c.getIndex() - nbCandles; i < c.getIndex() + nbCandles; i++) {
			if (c.getHigh() < candles.get(i).getHigh()) {
				return false;
			}
		}
		return true;
	}

	private Candle getImpulseStartMACD(Candle c, List<Candle> candles) {
		Candle impulseStart = c;
		for (int i=0; i< 100; i++) {
			Candle previousCandle = candles.get(impulseStart.getIndex() - 1);
			if (previousCandle.getMacdDiff() > impulseStart.getMacdDiff()) {
				break;
			}
			impulseStart = previousCandle;
		}
		return impulseStart;
	}

	private Optional<Extremum> findMaximum(Candle c, List<Extremum> extremums) {
		return extremums.stream()
				.filter(e -> Math.abs(e.candle.getIndex() - c.getIndex()) < 5)
				.findFirst();
	}

	private boolean isInside(Candle c, List<Candle> candlesHTF) {
		Candle cHTFFound = null;
		for (int i = 0; i < candlesHTF.size()-1; i++) {
			Candle cHTF = candlesHTF.get(i);
			Candle cHTFPlus1 = candlesHTF.get(i+1);
			if ((c.getDate().isAfter(cHTF.getDate()) || c.getDate().equals(cHTF.getDate()))
					&& c.getDate().isBefore(cHTFPlus1.getDate())) {
				cHTFFound = cHTF;
				break;
			}
		}
		if (cHTFFound != null) {
			if (cHTFFound.getLow() < cHTFFound.getEma()) {
				return false;
			}
			if (cHTFFound.getEmaSlope() < 0) {
				return false;
			}
			if (cHTFFound.getMacd() < 0) {
				return false;
			}
			if (cHTFFound.getMacdSignal() < 0) {
				return false;
			}
			//			if (cHTFFound.getMacdDiff() < 0) {
			//				return false;
			//			}
			return true;
		}
		return false;
	}

	private List<Candle> buildValidPrices(EnumTimeRange tr) {
		List<Candle> candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(
				market,
				tr,
				fetchStartDate,
				endDate);

		EmaCalculator.computeEMA(candles, 200);
		RsiCalculator.computeRSI(candles, 14);
		MacdCalculator.computeMacd(candles);
		List<Candle> candlesValid = new ArrayList<Candle>();
		for (int i = 0; i < candles.size(); i++) {
			Candle c = candles.get(i);
			if (c.getDate().isBefore(startDate)) {
				continue;
			}
			candlesValid.add(c);
			//			if (c.getLow() < c.getEma()) {
			//				continue;
			//			}
			//			if (c.getMacd() < 0 || c.getMacdSignal() < 0) {
			//				continue;
			//			}
			//			System.out.println("OK for " + c.getDate());
			//		}
		}
		return candlesValid;
	}
}

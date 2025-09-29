package com.trading.launcher;

import static java.util.Map.entry;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.trading.dto.DatePoint;
import com.trading.dto.HorizontalLine;
import com.trading.dto.HotSpotData;
import com.trading.dto.Range;
import com.trading.entity.Candle;
import com.trading.entity.HotSpot;
import com.trading.enums.EnumTimeRange;
import com.trading.indicator.AtrCalculator;
import com.trading.indicator.RsiCalculator;
import com.trading.indicator.extremum.Extremum;
import com.trading.indicator.extremum.SimpleMinMaxAnalyzer;
import com.trading.indicator.extremum.SwingExtremaFinder;
import com.trading.service.AbstractService;
import com.trading.service.DebugHolder;
import com.trading.service.PriceEmbargo;
import com.trading.service.RangeFinder;
import com.trading.service.old.FeatureWriterSpring;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile("trade-finder")
@Slf4j
public class TradeFinder extends AbstractService implements CommandLineRunner {


	private static final String HOTSPOT_CODE = "RANGE_AUTO";
	private static final double MIN_MAX_ATR_RATIO = 2.5d;
	private static final Double RATIO_RANGE_MAX_SPRING_DEPTH = 0.5;

	Map<Integer, String> mapReason = new ConcurrentHashMap<>();
	@Autowired
	RangeFinder rangeFinder;
	@Autowired
	FeatureWriterSpring featureWriter;
	SwingExtremaFinder analyzer = new SwingExtremaFinder();
//		EnumTimeRange timeRange = EnumTimeRange.M5;
//					EnumTimeRange timeRange = EnumTimeRange.M10;
	EnumTimeRange timeRange = EnumTimeRange.M15;
//			EnumTimeRange timeRange = EnumTimeRange.M30;
//		EnumTimeRange timeRange = EnumTimeRange.H1;
	Random random = new Random();
	ExecutorService executor = Executors.newFixedThreadPool(1);
	Map<String, Double> mapATR = new HashMap<String, Double>();
	//	private List<HotSpot> hotspotsToProcess;

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		System.out.println("Accumulation Break starting");
		backupHotSpots();
		//		hotSpotRepository.deleteByCode(HOTSPOT_CODE);
		hotSpotRepository.deleteByCode("RANGE_TO_AVOID");
		hotSpotRepository.deleteByCode("LOST_RANGE");
		hotSpotRepository.deleteByCode("WIN_RANGE");
		hotSpotRepository.deleteByCode("DOWN_RANGE");

		//		hotspotsToProcess = hotSpotRepository.findByCodeOrderByDateEnd("RANGE_LABEL")
		//				.stream()
		//				.filter(hs -> hs.getData().getLabels().contains("SPRING WIN"))
		//				.collect(Collectors.toList());
		//		List<HotSpot> hotSpotsToFind = new CopyOnWriteArrayList<HotSpot>();
		long startProcess = System.currentTimeMillis();
		Map<String, LocalDateTime> marketDateMap = Map
				//				.ofEntries(entry("US100", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0))
				.ofEntries(entry("GOLD", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0))

						// entry("GOLD", LocalDateTime.of(2010, Month.JANUARY, 1, 0, 0)),
						// entry("US100", LocalDateTime.of(2018, Month.MAY, 1, 0, 0))
						// ,entry("SILVER", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
						// entry("EURUSD", LocalDateTime.of(2015, Month.JANUARY, 1, 0, 0)),
						// entry("BRENT", LocalDateTime.of(2015, Month.JANUARY, 1, 0, 0)),
						// entry("COPPER", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
						// entry("USDJPY", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
						// entry("DAX30", LocalDateTime.of(2015, Month.JANUARY, 1, 0, 0)),
						// entry("ETHUSD", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
						// entry("BTCUSD", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
						// entry("GBPUSD", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
						// entry("US500", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
						// entry("US30", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
						// entry("AUDUSD", LocalDateTime.of(2015, Month.JANUARY, 1, 0, 0)),
						// entry("USDCHF", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
						// entry("WTI", LocalDateTime.of(2015, Month.JANUARY, 1, 0, 0)),
						// entry("CHINA", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0))
						);
		marketDateMap.forEach((market, startDate) -> {
			LocalDateTime endDate = null;
			endDate = LocalDateTime.of(2023, Month.DECEMBER, 31, 23, 59);
			while (startDate.isBefore(endDate)) {
				final LocalDateTime startDateFinal = startDate;
				executor.submit(() -> {
					try {
						process(market, startDateFinal, startDateFinal.plusMonths(1));
					} catch (Exception e) {
						log.error("Error processing {}", startDateFinal, e);
					}
				});
				startDate = startDate.plusMonths(1);
			}
		});
		executor.shutdown();
		if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
			executor.shutdownNow();
		}
		displayMapCount();

		long endProcess = System.currentTimeMillis();
		System.out.println("Total time processing " + (endProcess - startProcess) / 1000 + " sec");

	}

	private void process(String market, LocalDateTime startDate, LocalDateTime endDate) throws IOException {
		//		System.out.println("Process from " + startDate + " to " + endDate);
		List<HotSpot> hotSpotsToFind = hotSpotRepository.findByCodeAndTimeRangeAndMarketAndDateEndBetween(
				"SPRING_LABEL", timeRange, market, startDate, endDate);
		List<Candle> candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(market, timeRange,
				startDate.minusDays(50), endDate.plusDays(7));
		PriceEmbargo priceEmbargo = new PriceEmbargo(candles);

//		List<Candle> candlesHTF = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(market, EnumTimeRange.H1,
//				startDate.minusDays(100), endDate.plusDays(10));
//		setIndex(candlesHTF);
//		Map<LocalDateTime, Candle> mapCandlesHTF = candlesHTF.stream().collect(Collectors.toMap(c -> c.getDate(), c -> c));
//		EmaCalculator emaCalculator =  new EmaCalculator(new ArrayList<>(candlesHTF), 200);

		
//		RsiCalculator.computeRSI(candles, 14);
		List<Range> rangeList = new ArrayList<Range>();
		AtrCalculator atrCalculator = new AtrCalculator(20);
		
		SimpleMinMaxAnalyzer minMaxAnalyzer = new SimpleMinMaxAnalyzer(MIN_MAX_ATR_RATIO, 7);
		for (int i = 0; i < candles.size(); i++) {
			Candle c = candles.get(i);
			DebugHolder.activate(c);
			atrCalculator.compute(c);
//			System.out.println(c.atr);
			List<Extremum> extremumsSwing = minMaxAnalyzer.process(c);
			if (c.getDate().isBefore(startDate) || c.getDate().isAfter(endDate)) {
				continue;
			}

			Double atr = getDayBeforeAverageATR(candles, c);
			Range range = rangeFinder.getLargestRangeFast(candles, i, extremumsSwing, atr);
			if (range == null) {
				continue;
			}
			boolean isSpringBeforeLimit = isSpringBeforeLimit(range, candles, market);
			if (!isSpringBeforeLimit) {
				continue;
			}


			Optional<Range> existingRange = getRangeAlreadyExists(range, rangeList);
			if (existingRange.isPresent()) {
				DebugHolder.eliminated("Range already exists and starts at " + existingRange.get().getDateStart());
				continue;
			}

			DebugHolder.accepted(range);
			
			rangeList.add(range);
			//			increaseCount("RANGE");
//			int indexCandlePreviousHTF = mapCandlesHTF.get(range.getDateEnd().truncatedTo(ChronoUnit.HOURS)).getIndex()-1;
//			Candle candlePreviousHTF = candlesHTF.get(indexCandlePreviousHTF);
//			Double emaHTF = emaCalculator.get(candlePreviousHTF.getDate());
//			if (candlePreviousHTF.getMax() < emaHTF) {
////								continue;
//			}

			List<HorizontalLine> lines = List.of(buildLine(range.getDateStart(), range.getMin(), "#FFFFFF"),
					buildLine(range.getDateStart(), range.getMin() + range.getHeight()/2, "#FFFFFF"));
			HotSpotData data = HotSpotData.builder().lines(lines).build();
			if (isGoingDown(range, candles)) {
				increaseCount("GOING DOWN");
				saveHotSpot(range.getDateStart(), range.getDateEnd(), List.of(range.getDateStart(), range.getDateEnd()), market, timeRange, "DOWN_RANGE", data);
				System.out.println("GOING DOWN " + range.getDateEnd());
			} else if (isWinner(range, candles)) {
				saveHotSpot(range.getDateStart(), range.getDateEnd(), List.of(range.getDateStart(), range.getDateEnd()), market, timeRange, "WIN_RANGE", data);
				increaseCount("WIN");
				System.out.println("WIN " + range.getDateEnd());
			} else {
				saveHotSpot(range.getDateStart(), range.getDateEnd(), List.of(range.getDateStart(), range.getDateEnd()), market, timeRange, "LOST_RANGE", data);
				increaseCount("LOST");
				System.out.println("LOST " + range.getDateEnd());
			}
		}

		//			if (!hotSpotsToFind.stream().anyMatch(hs -> hs.getDateStart().equals(range.getDateStart()))) {
		//				System.out.println("Following range should not be found at " + range.getDateStart().format(formatter));
		//				saveHotSpot(range.getDateStart(), range.getDateEnd(), List.of(range.getDateStart()), market, timeRange, "RANGE_TO_AVOID", null);
		//							System.exit(0);
		//			}

		//						removeFromHotSpotToFind(range, hotSpotsToFind);

		//		stopIfNotEmpty(hotSpotsToFind);
	}

	private Optional<Range> getRangeAlreadyExists(Range range, List<Range> rangeList) {
		return rangeList.stream()
				.filter(r -> Math.abs(range.getIndexStart() - r.getIndexStart()) < 20 || Math.abs(range.getIndexEnd() - r.getIndexEnd()) < 20)
				.findFirst();
	}

	private boolean isGoingDown(Range range, List<Candle> candles) {
		if (candles.get(range.getIndexEnd()).getClose() > range.getMin()) {
			return false;
		}
		for(int i = range.getIndexEnd() + 1 ; i < Math.min(candles.size(), range.getIndexEnd() + 6); i++) {
			Candle c = candles.get(i);
			if (c.getMin() < range.getMin() - range.getHeight()*RATIO_RANGE_MAX_SPRING_DEPTH) {
				return true;
			}
			if (c.getMax() > range.getMin()) {
				return false;
			}
		}
		return true;
	}

	private boolean isWinner(Range range, List<Candle> candles) {
		Candle endCandle = candles.get(range.getIndexEnd());
		Double sl = endCandle.getMin();
		Integer startTradeIndex = null;
		if (endCandle.getClose() > range.getMin()) {
			startTradeIndex = endCandle.getIndex() + 1;
		} else {
			for(int i = 1; i < 6; i++) {
				Candle c = candles.get(endCandle.getIndex() + i);
				sl = Math.min(c.getMin(), sl);
				if (c.getMax() >= range.getMin()) {
					startTradeIndex = endCandle.getIndex() + i;
					break;
				}
			}
			if (startTradeIndex == null) {
				throw new RuntimeException("startTradeIndex should not be null");
			}
		}
		range.setDateStartTrade(candles.get(startTradeIndex).getDate());
		for(int i = startTradeIndex; i < candles.size(); i++) {
			Candle c = candles.get(i);
			if (c.getMin() < endCandle.getMin()) {
				return false;
			}
			Double min = range.getMin();
			double tp = min + range.getHeight()/2;
			if (c.getMax() >= tp) {
				double rr = (tp - min) / (min - sl);
				System.out.println(rr);
				return true;
			}
		}
		return false;
	}

	private boolean isSpringBeforeLimit(Range range, List<Candle> candles, String market) {
		List<Candle> candlesExtra = new ArrayList<>();
		candlesExtra.addAll(candles);
		if (candles.size() - range.getIndexEnd() < 100) {
			Candle lastCandle = candles.getLast();
			Integer lastCandleIndex = lastCandle.getIndex();
			List<Candle> moreCandles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(
					market, timeRange, lastCandle.getDate().plusSeconds(1), lastCandle.getDate().plusWeeks(1));
			candlesExtra.addAll(moreCandles);
			setIndex(candlesExtra);
			if (moreCandles.getFirst().getIndex() != lastCandleIndex + 1) {
				throw new RuntimeException("Something very weird");
			}
		}
		for(int i = range.getIndexEnd(); i < candlesExtra.size(); i++) {
			Candle c = candlesExtra.get(i);
			if (c.getHigh() > range.getMax() + range.getHeight()) {
				DebugHolder.eliminated("Range is going to high");
				return false;
			}
			if (c.getIndex() - range.getIndexStart() > RangeFinder.MAX_RANGE_WIDTH) {
				DebugHolder.eliminated("Range not reaching bottom before limit");
				return false;
			}
			if (c.getLow() < range.getMin() - c.getAtr()*0.5) {
				range.setDateEnd(c.getDate());
				range.setIndexEnd(c.getIndex());
				return true;
			}
		}
		throw new RuntimeException("Range is going exceeding the current period, it starts at " + range.getDateStart().format(formatter));
	}

	private void stopIfNotEmpty(List<HotSpot> hotSpotsToFind) {
		if (!hotSpotsToFind.isEmpty()) {
			HotSpot hs = hotSpotsToFind.get(0);
			saveHotSpot(hs.getDateStart(), hs.getDateEnd(), List.of(hs.getDateStart()), hs.getMarket(), timeRange, "RANGE_TO_FIX", null);
			System.out.println("Failed to find SPRING_LABEL starting at " + hs.getDateStart().format(formatter));
			System.exit(0);
		}
	}

	private void removeFromHotSpotToFind(Range range, List<HotSpot> hotSpotsToFind) {
		for (Iterator<HotSpot> it = hotSpotsToFind.iterator(); it.hasNext();) {
			HotSpot hs = it.next();
			if ( hs.getDateStart().equals(range.getDateStart()) && hs.getDateEnd().equals(range.getDateEnd())) {
				increaseCount("HS FOUND!!");
				it.remove();
			} 
		}
	}

	private Double getDayBeforeAverageATR(List<Candle> candles, Candle c) {
		LocalDateTime firstDayOfWeek = c.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).withHour(0)
				.withMinute(0).withSecond(0).withNano(0);
		String key = firstDayOfWeek + c.getMarket();
		if (mapATR.containsKey(key)) {
			return mapATR.get(key);
		}
		List<Candle> lastWeekCandles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(c.getMarket(),
				c.getTimeRange(), firstDayOfWeek.minusWeeks(1), firstDayOfWeek);
		if (lastWeekCandles.isEmpty()) {
			mapATR.put(key, c.atr);
			return c.atr;
		}
		double atr = rangeFinder.getAverageATR(candles, 0, lastWeekCandles.size() - 1);
		mapATR.put(key, atr);
		return atr;
	}

	private void saveHotSpotRange(Range range, String market) {
		List<HorizontalLine> lines = List.of(buildLine(range.getDateStart(), range.getMinWithLow(), "#FFFFFF"));
		List<LocalDateTime> keyDates = new ArrayList<>();
		keyDates.addAll(List.of(range.getDateStart(), range.getDateEnd()));
		keyDates.addAll(range.getMaxList().stream().map(Candle::getDate).toList());
		keyDates.addAll(range.getMinList().stream().map(Candle::getDate).toList());
		saveHotSpot(range.getDateStart(), range.getDateEnd(), keyDates, market, timeRange, HOTSPOT_CODE,
				HotSpotData.builder().lines(lines)
				.points(List.of(DatePoint.builder().date(range.getDateStart()).color("#FFFFFF").build(),
						DatePoint.builder().date(range.getDateEnd()).color("#FFFFFF").build()))
				.build());

	}

	private HorizontalLine buildLine(LocalDateTime date, Double price, String color) {
		return HorizontalLine.builder().color(color).dateStart(date).price(price).build();
	}


	private void checkHotSpotNotFound(List<Candle> candles, List<HotSpot> hotSpotsToFind) {
		for (HotSpot hotSpot : hotSpotsToFind) {
			Optional<Candle> endHotSpot = getIndexByDate(hotSpot.getDateEnd(), candles);
			if (endHotSpot.isPresent()) {
				for (Entry<Integer, String> entry : mapReason.entrySet()) {
					if (Math.abs(entry.getKey() - endHotSpot.get().getIndex()) < 20) {
						// System.out.println(hotSpot.getDateEnd() + " " + entry.getValue());
						break;
					}
				}
			}
		}
	}

	private void checkHotSpotFound(Range newRange, List<Candle> candles, List<HotSpot> hotSpotsToFind) {
		for (Iterator<HotSpot> iterator = hotSpotsToFind.iterator(); iterator.hasNext();) {
			HotSpot hotSpot = (HotSpot) iterator.next();
			// System.out.println(hotSpot.getDateEnd());
			Optional<Candle> endHotSpot = getIndexByDate(hotSpot.getDateEnd(), candles);
			Optional<Candle> startHotSpot = getIndexByDate(hotSpot.getDateStart(), candles);

			if (endHotSpot.isPresent() && startHotSpot.isPresent()
					&& Math.abs(newRange.getIndexEnd() - endHotSpot.get().getIndex()) < 10
					&& Math.abs(newRange.getIndexStart() - startHotSpot.get().getIndex()) < 10) {
				iterator.remove();
				// System.out.println("Accumulation found at " + hotSpot.getDateEnd());
				return;
			}

			long distance = Duration.between(hotSpot.getDateEnd(), newRange.getDateEnd()).abs().toMillis()
					/ timeRange.bucketMillis();
			if (distance < 10) {
				iterator.remove();
				System.out.println("Accumulation found at " + hotSpot.getDateEnd());
				return;
			}
		}
	}


	List<Integer> toIndexList(List<Candle> list) {
		return list.stream().map(c -> c.getIndex()).toList();
	}

	//	private Trade processTrade(List<Candle> candlesHTF, Range range) {
	//		List<Integer> minList = toIndexList(range.getMinList());
	//		List<Integer> maxList = toIndexList(range.getMaxList());
	//		Candle startCandleHTF = candlesHTF.get(range.getIndexStart());
	//		Candle actualCandleHTF = candlesHTF.get(range.getIndexRangeValidated() - 1);
	//		Integer firstMinIndex = minList.get(0);
	//		Candle firstMaxCandle = candlesHTF.get(maxList.get(0));
	//		if (firstMinIndex > firstMaxCandle.getIndex()) {
	//			DebugHolder.eliminated();
	//			return null;
	//		}
	//		double min = Double.MAX_VALUE;
	//		double max = Double.MIN_VALUE;
	//		double height = 0;
	//		List<Candle> candlesS30 = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(
	//				startCandleHTF.getMarket(), EnumTimeRange.S30, startCandleHTF.getDate(),
	//				candlesHTF.get(Math.min(range.getIndexStart() + 300, candlesHTF.size() - 1)).getDate());
	//		setIndex(candlesS30);
	//		AtrCalculator.compute(candlesS30, 20);
	//		boolean debug = false;
	//		Candle candleBreak = null;
	//		Double sl = min;
	//		Candle c0 = candlesS30.get(0);
	//		for (int i = 0; i < candlesS30.size(); i++) {
	//			Candle c = candlesS30.get(i);
	//			DebugHolder.info();
	//			height = max - min;
	//			if (c.getDate().isBefore(actualCandleHTF.getDate())) {
	//				min = Math.min(min, c.getLow());
	//				max = Math.max(max, c.getMax());
	//				height = max - min;
	//			} else {
	//				sl = Math.min(sl, c.getLow());
	//				double springUnderPrice = min - 0.5 * c.getAtr();
	//				double springAbovePrice = min + 0.5 * c.getAtr();
	//				if (candleBreak == null && c.getLow() < springUnderPrice) {
	//					candleBreak = c;
	//					List<HorizontalLine> lines = List.of(buildLine(c0.getDate(), min, "#00F53A"));
	//					//					saveHotSpot(c0.getDate(), c.getDate(), List.of(), c.getMarket(), EnumTimeRange.S30, HOTSPOT_CODE,
	//					//							HotSpotData.builder().lines(lines).build());
	//					increaseCount("SPRING");
	//					return new Trade();
	//					//					break;
	//				}
	//				if (candleBreak != null && c.getClose() > springAbovePrice) {
	//					LocalDateTime indexStart = candlesS30.get(c.getIndex() - 30).getDate();
	//					LocalDateTime indexEnd = candlesS30.get(Math.min(c.getIndex() + 5, candlesS30.size() - 1))
	//							.getDate();
	//
	//					int distance = c.getIndex() - candleBreak.getIndex();
	//					// if (distance > 50) {
	//					// return null;
	//					// }
	//					Candle cTradeStart = candlesS30.get(c.getIndex() + 1);
	//					Trade trade = Trade.builder().startDate(indexStart).endDate(indexEnd).timeRange(EnumTimeRange.S30)
	//							.rangeMin(min).rangeMax(max).sl(sl).tp(min + height / 4).springUnderPrice(springUnderPrice)
	//							.springAbovePrice(springAbovePrice).underRangeLimit(min - 0.1 * height)
	//							.aboveRange1(min + 0.05 * height).aboveRange2(min + 0.1 * height)
	//							.aboveRange3(min + 0.15 * height)
	//							.nbBottoms(calculateNbBottoms(candlesHTF, startCandleHTF, candleBreak, minList, min, max))
	//							.height(height).build();
	//					// addFeature(candlesS30, candleBreak, cTradeStart, trade, range);
	//					return trade;
	//				}
	//				//				if (c.getHigh() > max) {
	//				//					DebugHolder.eliminated();
	//				//					return null;
	//				//				}
	//				if (c.getLow() < min - height * 0.3) {
	//					DebugHolder.eliminated();
	//					return null;
	//				}
	//			}
	//		}
	//		return null;
	//	}



}

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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.trading.dto.DatePoint;
import com.trading.dto.HorizontalLine;
import com.trading.dto.HotSpotData;
import com.trading.dto.Range;
import com.trading.dto.Trade;
import com.trading.entity.Candle;
import com.trading.entity.HotSpot;
import com.trading.enums.EnumTimeRange;
import com.trading.enums.ExtremumType;
import com.trading.indicator.AtrCalculator;
import com.trading.indicator.extremum.Extremum;
import com.trading.indicator.extremum.SimpleMinMaxAnalyzer;
import com.trading.indicator.extremum.SwingExtremaFinder;
import com.trading.service.AbstractService;
import com.trading.service.DebugHolder;
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

	Map<Integer, String> mapReason = new ConcurrentHashMap<>();
	@Autowired
	RangeFinder rangeFinder;
	@Autowired
	FeatureWriterSpring featureWriter;
	SwingExtremaFinder analyzer = new SwingExtremaFinder();
	EnumTimeRange timeRange = EnumTimeRange.M5;
//	EnumTimeRange timeRange = EnumTimeRange.M10;
//	EnumTimeRange timeRange = EnumTimeRange.M15;
//	EnumTimeRange timeRange = EnumTimeRange.M30;
	Random random = new Random();
	ExecutorService executor = Executors.newFixedThreadPool(1);
	Map<String, Double> mapATR = new HashMap<String, Double>();
	//	private List<HotSpot> hotspotsToProcess;

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		System.out.println("Accumulation Break starting");
		backupHotSpots();
		hotSpotRepository.deleteByCode(HOTSPOT_CODE);
		//		hotspotsToProcess = hotSpotRepository.findByCodeOrderByDateEnd("RANGE_LABEL")
		//				.stream()
		//				.filter(hs -> hs.getData().getLabels().contains("SPRING WIN"))
		//				.collect(Collectors.toList());
		List<HotSpot> hotSpotsToFind = new CopyOnWriteArrayList<HotSpot>();
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
						hotSpotsToFind.addAll(process(market, startDateFinal, startDateFinal.plusMonths(1)));
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

	private List<HotSpot> process(String market, LocalDateTime startDate, LocalDateTime endDate) throws IOException {
		List<HotSpot> hotSpotsToFind = hotSpotRepository.findByCodeAndTimeRangeAndMarketAndDateEndBetween(
				"SPRING_LABEL", timeRange, market, startDate, endDate);
		List<Candle> candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(market, timeRange,
				startDate.minusDays(50), endDate.plusDays(1));
		setIndex(candles);
		AtrCalculator.compute(candles, 20);
		Map<Integer, Range> rangeStartMap = new HashMap<Integer, Range>();
		SimpleMinMaxAnalyzer minMaxAnalyzer = new SimpleMinMaxAnalyzer(MIN_MAX_ATR_RATIO, 7);
		for (int i = 0; i < candles.size(); i++) {
			Candle c = candles.get(i);
			DebugHolder.activate(c);
			List<Extremum> extremumsSwing = minMaxAnalyzer.process(c);
			if (c.getDate().isBefore(startDate) || c.getDate().isAfter(endDate)) {
				continue;
			}

			Double atr = getDayBeforeAverageATR(candles, c);
			Range range = rangeFinder.getLargestRangeFast(candles, i, extremumsSwing, atr);
			if (range == null) {
				continue;
			}
			Optional<Integer> existingRangeIndex = getRangeAlreadyExists(range, rangeStartMap);
			if (existingRangeIndex.isPresent()) {
				Range existingRange = rangeStartMap.get(existingRangeIndex.get());
				DebugHolder.eliminated("Range already exists and starts at " + existingRange.getDateStart());
				continue;
			}
			DebugHolder.accepted(range);
			rangeStartMap.put(range.getIndexStart(), range);
			for (Iterator<HotSpot> it = hotSpotsToFind.iterator(); it.hasNext();) {
				HotSpot hs = it.next();
				if ( hs.getDateStart().equals(range.getDateStart())) {
					increaseCount("HS FOUND!!");
					it.remove();
				} 
			}
		}
		if (!hotSpotsToFind.isEmpty()) {
			System.out.println("Failed to find SPRING_LABEL starting at " + hotSpotsToFind.get(0).getDateStart().format(formatter));
			System.exit(0);
		}
		return hotSpotsToFind;
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

	private Optional<Integer> getRangeAlreadyExists(Range range, Map<Integer, Range> rangeStartMap) {
		return rangeStartMap.keySet().stream().filter(index -> Math.abs(range.getIndexStart() - index) < 20).findFirst();
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

	private Integer calculateNbBottoms(List<Candle> candlesHTF, Candle startCandleHTF, Candle candleBreakS30,
			List<Integer> minList, double min, double max) {
		double rangeHeight = max - min;
		double middle = min + 0.5 * rangeHeight;
		double bottomBandLimit = min + RangeFinder.BOTTOM_BAND_RATIO * rangeHeight;
		boolean ready = true;
		int nbBottoms = 0;
		for (int i = startCandleHTF.getIndex(); i < candlesHTF.size(); i++) {
			Candle c = candlesHTF.get(i);
			// System.out.println(c.getDate());
			if (c.getDate().isAfter(candleBreakS30.getDate())) {
				break;
			}
			if (ready && c.getLow() <= bottomBandLimit) {
				nbBottoms++;
				ready = false;
			}
			if (c.getClose() >= middle) {
				ready = true;
			}
		}
		// System.out.println(candleBreakS30.getDate() + " " + nbBottoms);
		return nbBottoms;
	}

	private void addFeature(List<Candle> candlesS30, Candle cBreak, Candle cTradeStart, Trade trade, Range range) {
		boolean isTP = false;
		double rr = (trade.getTp() - cTradeStart.getOpen()) / (cTradeStart.getOpen() - trade.getSl());
		Candle endCandle = null;
		if (!Double.isFinite(rr) || rr < 1) {
			return;
		}
		for (int i = cTradeStart.getIndex(); i < candlesS30.size(); i++) {
			Candle c = candlesS30.get(i);
			if (c.getHigh() >= trade.getTp()) {
				isTP = true;
				endCandle = c;
				// increaseCount("TP");
				// increaseDouble("RESULT", rr);
				break;
			}
			if (c.getLow() <= trade.getRangeMin() - trade.getHeight() * 0.3) {
				// if (c.getLow() <= trade.getSl()) {
				isTP = false;
				endCandle = c;
				// increaseCount("SL");
				// increaseDouble("RESULT", -1d);
				return;
			}
		}
		if (endCandle == null) {
			return;
		}
		// if (trade.getNbBottoms() <= 2) {
		// return;
		// }
		List<HorizontalLine> lines = List.of(buildLine(trade.getStartDate(), trade.getRangeMin(), "#FFFFFF"),
				// buildLine(trade.getStartDate(), trade.getSl(), "#F54927"),
				buildLine(trade.getStartDate(), trade.getTp(), "#00F53A"),
				buildLine(trade.getStartDate(), trade.getSpringAbovePrice(), "#EBB731"),
				buildLine(trade.getStartDate(), trade.getSpringUnderPrice(), "#EBB731"));
		saveHotSpot(trade.getStartDate(), endCandle.getDate(), List.of(cTradeStart.getDate(), endCandle.getDate()),
				cBreak.getMarket(), EnumTimeRange.S30, HOTSPOT_CODE,
				HotSpotData.builder().lines(lines)
				.points(List.of(DatePoint.builder().date(cTradeStart.getDate()).color("#FFFF3D").build()))
				.build());
		// double breakSize = (trade.getRangeMin() - trade.getSl())/trade.getHeight();
		// Map<String, Object> mapFeature = new HashMap<String, Object>();
		// mapFeature.put("id", cBreak.getMarket() + "_" +
		// Utils.getTimestamp(cBreak.getDate()));
		// mapFeature.put("timestamp", Utils.getTimestamp(cBreak.getDate()));
		// mapFeature.put("f_time_since_break", cTradeStart.getIndex() -
		// cBreak.getIndex());
		////		mapFeature.put("f_rr", rr);
		// mapFeature.put("f_range_width", range.getWidth());
		// mapFeature.put("f_nb_bottoms", trade.getNbBottoms());
		// mapFeature.put("f_range_height", trade.getHeight() /
		// range.getMinList().get(0).getAtr());
		////		mapFeature.put("f_breakSize", breakSize);
		// mapFeature.put("f_timestamp", Utils.getTimestamp(cBreak.getDate()));
		// mapFeature.put("y", isTP ? 1d : 0d);
		// addPriceFeature(mapFeature, candlesS30, cBreak.getIndex(),
		// cTradeStart.getIndex()-1, trade);
		// listMapFeatures.add(mapFeature);
	}

	// double springConfirmationPrice = min + RATION_SPRING_CONFIRMATION * height;
	// System.out.println("New trade at " + range.getDateStart());
	// LocalDateTime indexStart = candlesS30.get(c.getIndex()-30).getDate();
	// LocalDateTime indexEnd = candlesS30.get(Math.min(c.getIndex()+5,
	// candlesS30.size()-1)).getDate();
	// return Trade.builder()
	// .startDate(indexStart)
	// .endDate(indexEnd)
	// .timeRange(EnumTimeRange.S30)
	// .rangeMin(min)
	// .springBreakPrice(min - c.getAtr())
	// .underRangeLimit(min - 0.1 * height)
	// .aboveRange1(min + 0.05 * height)
	// .aboveRange2(min + 0.1 * height)
	// .aboveRange3(min + 0.15 * height)
	// .build();

	// private List<Spring> processSprings(List<Candle> candles, int startIndex, int
	// endIndex, List<Integer> minList, List<Integer> maxList) {
	// Integer firstMinIndex = minList.get(0);
	// Integer firstMaxIndex = maxList.get(0);
	// if (firstMinIndex > firstMaxIndex) {
	// return List.of();
	// }
	// List<Spring> springs = new ArrayList<>();
	// double min = Double.MAX_VALUE;
	// double max = Double.MIN_VALUE;
	// double height = 0;
	// Candle firstMax = null;
	// Spring spring = null;
	// for (int i = startIndex; i <= endIndex; i++) {
	// Candle c = candles.get(i);
	// height = max - min;
	////			if (firstMax == null) {
	////				min = Math.min(min, c.getLow());
	////				max = Math.max(max, c.getMax());
	////				height = max - min;
	////				if (maxList.contains(i) && i != firstMinIndex) {
	////					double bottomBandPrice = min + height * BAND_RATIO;
	////					if (c.getMin() <= bottomBandPrice) {
	////						//Now second min, we can start looking for Spring
	////						firstMax = c;
	////					}
	////				}
	////			} else  {
	// if (i <= firstMaxIndex) {
	// min = Math.min(min, c.getLow());
	// max = Math.max(max, c.getMax());
	// height = max - min;
	// } else {
	// double springConfirmationPrice = min + RATION_SPRING_CONFIRMATION * height;
	// if (c.getLow() < min) {
	////					System.out.println(c.getDate());
	// if (spring == null) {
	// spring = new Spring();
	// spring.setDateBreak(c.getDate());
	// spring.setConfirmationPrice(springConfirmationPrice);
	// spring.setP1(min + 0.1 * height);
	// spring.setP2(min + 0.05 * height);
	// spring.setMin(min);
	// spring.setP3(min - 0.3 * height);
	// springs.add(spring);
	// }
	// if (c.getClose() < min && spring.getIndexBreak() == null) {
	// spring.setIndexBreak(i);
	// }
	// }
	// if (spring != null && c.getClose() > springConfirmationPrice) {
	// if (spring.getIndexBreak() == null || (i - spring.getIndexBreak() < 5)) {
	// spring.setConfirmed(true);
	// spring = null;
	// }
	// }
	// }
	//
	// }
	// return springs;
	// }

}

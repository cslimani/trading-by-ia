package com.trading.launcher;

import static java.util.Map.entry;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
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
import com.trading.indicator.MedianCalculator;
import com.trading.indicator.extremum.Extremum;
import com.trading.indicator.extremum.HybridMinMaxAnalyzer;
import com.trading.indicator.extremum.SwingExtremaFinder;
import com.trading.service.AbstractService;
import com.trading.service.FeatureWriterSpring;
import com.trading.service.Utils;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile("swing-accumulation")
@Slf4j
public class SwingAccumulation extends AbstractService implements CommandLineRunner {

	private static final int MIN_RANGE_WIDTH = 30;
	private static final Double MAX_RANGE_ATR_RATIO = 4.5d;
	private static final Double MIN_RANGE_ATR_RATIO = 1d;
	private static final String HOTSPOT_CODE = "TRADE";
	private static final double BAND_RATIO = 0.2d;
	private static final double MIN_MAX_ATR_RATIO = 2d;
	private static final Integer NB_CANDLES_BEFORE = 50;
	private static final double BREAK_DOWN_ATR_RATIO = 1;
	private static final double RATION_SPRING_CONFIRMATION = 0.1;
	private static final double BREAK_UP_ATR_RATIO = 0.5;
	private static final double NB_BOTTOMS_REQUIRED = 2;

	Map<Integer, String> mapReason = new ConcurrentHashMap<>();
	@Autowired
	FeatureWriterSpring featureWriter;
	SwingExtremaFinder analyzer = new SwingExtremaFinder();

	EnumTimeRange timeRange = EnumTimeRange.M5;
	Random random = new Random();
	ExecutorService executor = Executors.newFixedThreadPool(15);

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		boolean isTrainData = true;
		System.out.println("Accumulation Break starting with train data => " + isTrainData);
		backupHotSpots();
		hotSpotRepository.deleteByCode(HOTSPOT_CODE);
		List<HotSpot> hotSpotsToFind = new CopyOnWriteArrayList<HotSpot>();
		long startProcess = System.currentTimeMillis();
		Map<String, LocalDateTime> marketDateMap = Map.ofEntries(
				entry("GOLD",   LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0))

//																entry("GOLD",   LocalDateTime.of(2010, Month.JANUARY, 1, 0, 0)),
//																entry("US100",  LocalDateTime.of(2018, Month.MAY, 1, 0, 0))
//																,entry("SILVER",   LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//																entry("EURUSD", LocalDateTime.of(2015, Month.JANUARY, 1, 0, 0)),
//																entry("BRENT",  LocalDateTime.of(2015, Month.JANUARY, 1, 0, 0)),
//																entry("COPPER", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//																entry("USDJPY", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//																entry("DAX30",  LocalDateTime.of(2015, Month.JANUARY, 1, 0, 0)),
//																entry("ETHUSD", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//																entry("BTCUSD", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//																entry("GBPUSD", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//																entry("US500", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//																entry("US30", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//																entry("AUDUSD", LocalDateTime.of(2015, Month.JANUARY, 1, 0, 0)),
//																entry("USDCHF", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//																entry("WTI", LocalDateTime.of(2015, Month.JANUARY, 1, 0, 0)),
//																entry("CHINA", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0))
				);
		marketDateMap.forEach((market, startDate) -> {
			LocalDateTime endDate = null;
			if (isTrainData) {
				endDate = LocalDateTime.of(2023, Month.DECEMBER, 31, 23, 59);
				
			} else {
				startDate = LocalDateTime.of(2024, Month.JANUARY, 1, 0, 0);
				endDate = LocalDateTime.of(2025, Month.DECEMBER, 31, 23, 59);
			}
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
		
		setSourceFileName(isTrainData ?  "train" : "test");
		backupFile();
		writeAll();
		long endProcess = System.currentTimeMillis();
		System.out.println("Total time processing " + (endProcess - startProcess)/1000 + " sec");

		//		System.out.println("-----------------------------------------------------------------------------------------------------------");
		//		System.out.println();
		//		System.out.println("HotSpots to find " + hotSpotsToFind.size());
		//		System.out.println(hotSpotsToFind.stream().map(hs -> hs.getDateEnd()).toList());
		//		System.out.println();
		//		System.out.println("-----------------------------------------------------------------------------------------------------------");

	}


	private List<HotSpot> process(String market, LocalDateTime startDate, LocalDateTime endDate) throws IOException {
		//		System.out.println("Start processing " + startDate + " for " + market);
		List<HotSpot> hotSpotsToFind = hotSpotRepository.findByCodeAndTimeRangeAndMarketAndDateEndBetween("RANGE_TO_FIND", timeRange, market, startDate, endDate);
		List<Candle> candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(
				market,
				timeRange,
				startDate.minusDays(10),
				endDate.plusDays(1));
		setIndex(candles);
		AtrCalculator.compute(candles, 50);
		List<Integer> rangeStartList = new ArrayList<>();
		//		List<Extremum> extremumsSwing = analyzer.findExtrema(candles, 7, false);
		//		List<Extremum> extremumsEntry = analyzer.findExtrema(candles, 5, false);
		//		List<Integer> swingHighList = new ArrayList<>();
		HybridMinMaxAnalyzer minMaxAnalyzer = new HybridMinMaxAnalyzer(MIN_MAX_ATR_RATIO, 7);
		Range range = null;
		for (int i = 0; i < candles.size(); i++) {
			Candle c = candles.get(i);
			if (c.getDate().isBefore(startDate) || c.getDate().isAfter(endDate) ) {
				continue;
			}
			List<Extremum> extremumsSwing = minMaxAnalyzer.process(c);
			if (range == null) {
				//				Double averageATR = AtrCalculator.average(candles, i, 50);
				//				Double maxRangeHeight = MAX_RANGE_ATR_RATIO*c.getAtr();
				//				if (isInvalid(List.of(maxRangeHeight, c.getAtr()))) {
				//					continue;
				//				}

				Range newRange = getLargestRangeFast(candles, i, extremumsSwing);
				if (newRange == null) {
					continue;
				}
				//				if (newRange.getDateStart().isEqual(LocalDateTime.of(2020, 9, 25, 12, 20))) {
				//					getClass();
				//				}
				if (!isConditionOnRangeValid(newRange, extremumsSwing, candles)) {
					continue;
				}
				//				if (newRange.getSprings().isEmpty()) {
				//					continue;
				//				}
				//				if (newRange.getSprings().stream().anyMatch(sp -> sp.isConfirmed())) {
				//					continue;
				//				}
				//				if (newRange.getBreakFromTop()) {
				//					continue;
				//				}
				if (rangeAlreadyExists(newRange, rangeStartList)) {
					continue;
				}

				Trade trade = processTrade(candles, newRange);
				if (trade == null) {
					continue;
				}
				increaseCount("_RANGE_OK");
				//				System.out.println("New Range ok with end date" + newRange.getDateEnd());
				//				checkHotSpotFound(newRange, candles, hotSpotsToFind);
				List<LocalDateTime> keyDates = new ArrayList<>();
				keyDates.addAll(List.of(newRange.getDateStart(), newRange.getDateEnd()));
				keyDates.addAll(newRange.getMaxList().stream().map(Candle::getDate).toList());
				keyDates.addAll(newRange.getMinList().stream().map(Candle::getDate).toList());
				//				List<DatePoint> points = newRange.getSprings().stream()
				//						.map(sp -> DatePoint.builder().date(sp.getDateBreak()).color("#A9F527").build()).collect(Collectors.toList());
				//				//				points.add(DatePoint.builder().date(newRange.getDateDecisionRangeValid()).color("#8E8EE6").build());
				//				points.add(DatePoint.builder().date(c.getDate()).color("#FFFFFF").build());
				//
				//				List<HorizontalLine> lines = List.of(HorizontalLine.builder()
				//						.color("#FFFFFF")
				//						.dateStart(newRange.getDateStart())
				//						.price(newRange.getSprings().get(0).getConfirmationPrice()).build()
				//
				//						,HorizontalLine.builder()
				//						.color("#FFFFFF")
				//						.dateStart(newRange.getDateStart())
				//						.price(newRange.getSprings().get(0).getP1()).build()
				//
				//						,HorizontalLine.builder()
				//						.color("#FFFFFF")
				//						.dateStart(newRange.getDateStart())
				//						.price(newRange.getSprings().get(0).getP2()).build()
				//
				//						,HorizontalLine.builder()
				//						.color("#68F527")
				//						.dateStart(newRange.getDateStart())
				//						.price(newRange.getSprings().get(0).getMin()).build()
				//
				//						,HorizontalLine.builder()
				//						.color("#F54927")
				//						.dateStart(newRange.getDateStart())
				//						.price(newRange.getSprings().get(0).getP3()).build()
				//						);

				//				saveHotSpot(newRange.getDateStart(),
				//						newRange.getDateEnd(), 
				//						keyDates,
				//						market,
				//						timeRange,
				//						HOTSPOT_CODE,
				//						HotSpotData.builder()
				//						.lines(lines)
				//						.points(points)
				//						.build());


				List<HorizontalLine> lines = List.of(
						buildLine(newRange.getDateStart(), trade.getRangeMin(), "#00F53A"),
						buildLine(newRange.getDateStart(), trade.getSpringUnderPrice(), "#FFFFFF"),
						buildLine(newRange.getDateStart(), trade.getUnderRangeLimit(), "#F54927"),
						buildLine(newRange.getDateStart(), trade.getAboveRange1(), "#FFFFFF"),
						buildLine(newRange.getDateStart(), trade.getAboveRange2(), "#FFFFFF"),
						buildLine(newRange.getDateStart(), trade.getAboveRange3(), "#FFFFFF")
						);
//				saveHotSpot(trade.getStartDate(),
//						trade.getEndDate(), 
//						keyDates,
//						market,
//						EnumTimeRange.S30,
//						HOTSPOT_CODE,
//						HotSpotData.builder()
//						.lines(lines)
//						.build());
				rangeStartList.add(newRange.getIndexStart());
			} else {
			}
		}
		checkHotSpotNotFound(candles, hotSpotsToFind);
		return hotSpotsToFind;
	}


	private HorizontalLine buildLine(LocalDateTime date, Double price, String color) {
		return HorizontalLine.builder()
				.color(color)
				.dateStart(date)
				.price(price).build();
	}


	private boolean rangeAlreadyExists(Range range, List<Integer> rangeStartList) {
		return rangeStartList.stream()
				.anyMatch(index -> Math.abs(range.getIndexStart() - index) < 100);
	}


	private void checkHotSpotNotFound(List<Candle> candles, List<HotSpot> hotSpotsToFind) {
		for (HotSpot hotSpot : hotSpotsToFind) {
			Optional<Candle> endHotSpot = getIndexByDate(hotSpot.getDateEnd(), candles);
			if (endHotSpot.isPresent()) {
				for (Entry<Integer, String> entry : mapReason.entrySet()) {
					if (Math.abs(entry.getKey() - endHotSpot.get().getIndex()) < 20) {
						//						System.out.println(hotSpot.getDateEnd() + " " + entry.getValue());
						break;
					}
				}
			}
		}
	}


	private void checkHotSpotFound(Range newRange, List<Candle> candles, List<HotSpot> hotSpotsToFind) {
		for (Iterator<HotSpot> iterator = hotSpotsToFind.iterator(); iterator.hasNext();) {
			HotSpot hotSpot = (HotSpot) iterator.next();
			//			System.out.println(hotSpot.getDateEnd());
			Optional<Candle> endHotSpot = getIndexByDate(hotSpot.getDateEnd(), candles);
			Optional<Candle> startHotSpot = getIndexByDate(hotSpot.getDateStart(), candles);

			if (endHotSpot.isPresent() && startHotSpot.isPresent() &&
					Math.abs(newRange.getIndexEnd() - endHotSpot.get().getIndex()) < 10 &&
					Math.abs(newRange.getIndexStart() - startHotSpot.get().getIndex()) < 10) {
				iterator.remove();
				//				System.out.println("Accumulation found at " + hotSpot.getDateEnd());
				return;
			}

			long distance = Duration.between(hotSpot.getDateEnd(), newRange.getDateEnd()).abs().toMillis() / timeRange.bucketMillis();
			if (distance < 10) {
				iterator.remove();
				System.out.println("Accumulation found at " + hotSpot.getDateEnd());
				return;
			}
		}
	}


	private boolean isConditionOnRangeValid(Range range, List<Extremum> extremumsSwing, List<Candle> candles) {
		if (debug) {
			getClass();
		}
		double median = MedianCalculator.median(candles.subList(range.indexStart, range.indexEnd));
		Optional<Extremum> maxbeforeOpt = getFirstExtremumBefore(candles.get(range.getIndexStart()), extremumsSwing, ExtremumType.MAX, false);
		if (maxbeforeOpt.isEmpty()) {
			return false;
		}
		for (int i = range.getIndexStart() - NB_CANDLES_BEFORE; i < maxbeforeOpt.get().getIndex() ; i++) {
			if (i < 0 ) {
				return false;
			}
			if (candles.get(i).getLow() < median) {
				mapReason.put(range.getIndexEnd(), "CANDLES BEFORE");
				return false;
			}
		}
		int width = range.getIndexEnd() - range.getIndexStart();
		if (width < MIN_RANGE_WIDTH) {
			mapReason.put(range.getIndexEnd(), "WIDTH < 30");
			return false;
		}
		if (width > 150) {
			mapReason.put(range.getIndexEnd(), "WIDTH > 150");
			return false;
		}
		return true;
	}


	private Range getLargestRangeFast(List<Candle> candles, int actualIndex, List<Extremum> extremumsSwing) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;

		List<Extremum> extremumsReversed = extremumsSwing.stream()
				.filter(e -> e.getIndex() < actualIndex)
				.sorted(Comparator.comparingInt(Extremum::getIndex).reversed())
				.toList();
		List<Candle> minList = new ArrayList<Candle>();
		List<Candle> maxList = new ArrayList<Candle>();
		Candle tmp = candles.get(actualIndex);
		//		if (tmp.isDate(25, 9) && tmp.isTime(17, 25, 0)) {
		//			System.out.println();
		//			System.out.println();
		//			System.out.println();
		//			debug = true;
		//		}
		for (Extremum e :extremumsReversed) {
			Candle c = e.getCandle();
			if (isRangeValid(minList, maxList, actualIndex)) {
				debug(c.getDate() + " return valid range");
				return buildRange(candles, actualIndex, min, max, minList, maxList, extremumsReversed, c.getDate());
			}

			double averageATR = getAverageATR(candles, e.getIndex(), actualIndex);
			//			double medianATR = getMedianATR(candles, e.getIndex(), actualIndex);

			double maxRangeHeight = averageATR*MAX_RANGE_ATR_RATIO;
			debug("Looking at Extremum " + e.getDate());

			if (e.isMax()) {
				//				if (c.isDate(25, 8) && c.isTime(13, 25, 0)) {
				//					getClass();
				//				}
				if (!maxList.isEmpty()) {
					double potentialRangeSize = c.getMax() - min;
					if (potentialRangeSize > maxRangeHeight) {
						boolean rescueTop = false;
						Candle maxTop = getMaxOfTops(maxList);
						if (Math.abs(maxTop.getMax() - c.getMax()) < BREAK_UP_ATR_RATIO *c.getAtr()) {
							rescueTop = true;
							c.setRescued(true);
						}
						if (!rescueTop) {
							debug(c.getDate() + " new max out of bounds");
							//							if (isRangeValid(minList, maxList, actualIndex)) {
							//								debug(c.getDate() + " return valid range");
							////								return buildRange(candles, actualIndex, min, max, minList, maxList, extremumsReversed);
							//							} else {
							//								return null;
							//							}
							return null;
						}
					} else {
						debug(c.getDate() + " is valid max");
					}
				}
				maxList.add(e.getCandle());
				if (c.getMax() > max) {
					max = c.getMax();
				}
			}
			if (e.isMin()) {
				if (!minList.isEmpty()) {
					double potentialRangeSize = max - c.getMin();
					if (potentialRangeSize > maxRangeHeight) {
						boolean rescueBottom = false;
						Candle minBottom = getMinOfBottoms(minList);
						if (Math.abs(minBottom.getMin() - c.getMin()) < BREAK_DOWN_ATR_RATIO *c.getAtr()) {
							rescueBottom = true;
							c.setRescued(true);
						}
						if (!rescueBottom) {
							mapReason.put(actualIndex, "BREAK FROM BOTTOM");
							//							debug(c.getDate() + " new min out of bounds");
							//							if (isRangeValid(minList, maxList, actualIndex)) {
							//								return null;
							//								//						return buildRange(candles, endIndex, min, max, minList, maxList, extremumsReversed);
							//							} else {
							//								return null;
							//							}
							return null;
						}
					} else {
						debug(c.getDate() + " is valid min");
					}
				}
				minList.add(e.getCandle());
				if (c.getMin() < min) {
					min = c.getMin();
				}
			}
		}
		debug(" return no range");
		return null;
	}


	private double getAverageATR(List<Candle> candles, Integer startIndex, int endIndex) {
		double average = 0;
		for (int i = startIndex; i <= endIndex; i++) {
			average += candles.get(i).getAtr();
		}
		return average/(endIndex - startIndex + 1);
	}

	//	private double getMedianATR(List<Candle> candles, Integer startIndex, int endIndex) {
	//		List<Double> atrList= new ArrayList<Double>();
	//		for (int i = startIndex; i <= endIndex; i++) {
	//			atrList.add(candles.get(i).getAtr());
	//		}
	//		Collections.sort(atrList);
	//		return atrList.get(atrList.size()/2);
	//	}

	private Candle getMinOfBottoms(List<Candle> minList) {
		return minList.stream()
				.filter(c -> !c.isRescued())
				.sorted(Comparator.comparingDouble(Candle::getMin)).findFirst().get();
	}


	private Candle getMaxOfTops(List<Candle> maxList) {
		return maxList.stream()
				.filter(c -> !c.isRescued())
				.sorted(Comparator.comparingDouble(Candle::getMax).reversed()).findFirst().get();
	}


	private Range buildRange(List<Candle> candles, int actualIndex, double min, double max,
			List<Candle> minList, List<Candle> maxList, List<Extremum> extremumsReversed, LocalDateTime dateDecisionRangeValid) {
		Candle firstCandle = getFirstExtremum(minList, maxList);
		Candle lastCandle = getLastExtremum(minList, maxList);
		int endIndex = -1;
		Integer firstIndex = firstCandle.getIndex();
		Boolean breakFromTop = null;
		double height = max - min;
		double bottomLimit = min + 0.3 * height;
		//First index
		int startIndex = -1;
		for (int i = 1; i < firstIndex; i++) {
			Candle cTmp = candles.get(firstIndex - i);
			if (cTmp.getMin() < min - BREAK_DOWN_ATR_RATIO * cTmp.getAtr()) {
				mapReason.put(actualIndex, "START FROM BOTTOM");
				return null;
			}
			if (cTmp.getMax() > max + BREAK_UP_ATR_RATIO * cTmp.getAtr()) {
				startIndex = firstIndex -i +1;
				break;
			}
		}

		//Last index
		Integer lastIndex = lastCandle.getIndex();
		for (int i = 1; lastIndex + i < candles.size(); i++) {
			int index = lastIndex + i;
			Candle cTmpPrevious = candles.get(index - 1);
			Candle cTmp = candles.get(index);
			if (Math.abs(cTmp.getOpen() - cTmpPrevious.getClose()) > 2*cTmp.getAtr()) {
				return null;				
			}
			if (cTmp.getLow() < bottomLimit) {
				endIndex = index;
				breakFromTop = false;
				break;
			}
			if (cTmp.getMax() > max + BREAK_UP_ATR_RATIO * cTmp.getAtr()) {
				endIndex = index;
				breakFromTop = true;
				break;
			}
			//			max = Math.max(max, cTmp.getMax());
			//			min = Math.min(min, cTmp.getMin());
		}
		if (startIndex < 0) {
			return null;
		}
		if (endIndex < 0) {
			return null;
		}
		if (startIndex >= endIndex) {
			return null;
		}

		return Range.builder()
				.indexEnd(endIndex)
				.dateStart(candles.get(startIndex).getDate())
				.dateEnd(candles.get(endIndex).getDate())
				.indexStart(startIndex)
				.indexRangeValidated(actualIndex)
				.breakFromTop(breakFromTop)
				.min(min)
				.width(endIndex - startIndex)
				.dateDecisionRangeValid(dateDecisionRangeValid)
				.minList(minList)
				.maxList(maxList)
				.max(max)
				.build();
	}

	List<Integer> toIndexList(List<Candle> list){
		return list.stream().map(c -> c.getIndex()).toList();
	}

	private Trade processTrade(List<Candle> candlesHTF, Range range) {
		List<Integer> minList = toIndexList(range.getMinList());
		List<Integer> maxList = toIndexList(range.getMaxList());
		Candle startCandleHTF = candlesHTF.get(range.getIndexStart());
		Candle actualCandleHTF = candlesHTF.get(Math.min(candlesHTF.size()-1, range.getIndexRangeValidated()+1));
		Candle endCandleHTF = candlesHTF.get(candlesHTF.size()-1);
		Integer firstMinIndex = minList.get(0);
		Candle firstMaxCandle = candlesHTF.get(maxList.get(0));
		if (firstMinIndex > firstMaxCandle.getIndex()) {
			return null;
		}
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		double height = 0;
		List<Candle> candlesS30 = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(
				startCandleHTF.getMarket(),
				EnumTimeRange.S30,
				startCandleHTF.getDate(),
				candlesHTF.get(Math.min(range.getIndexStart() + 300, candlesHTF.size()-1)).getDate());
		setIndex(candlesS30);
		AtrCalculator.compute(candlesS30, 20);
		boolean debug = false;
		Candle candleBreak = null;
		Double sl = min;
		for (int i = 0;  i < candlesS30.size(); i++) {
			Candle c = candlesS30.get(i);
			if (c.isDate(11, 2) && c.isTime(3, 45, 0)) {
				debug = true;
			}
			height = max - min;
			if (c.getDate().isBefore(actualCandleHTF.getDate())) {
				min = Math.min(min, c.getLow());
				max = Math.max(max, c.getMax());
				height = max - min;
			} else {
				sl = Math.min(sl, c.getLow());
				double springUnderPrice = min - 0.5*c.getAtr();
				double springAbovePrice = min + 0.5*c.getAtr();
				if (candleBreak == null && c.getLow() < springUnderPrice) {
					candleBreak = c;
				}
				if (candleBreak != null && c.getClose() > springAbovePrice) {
					LocalDateTime indexStart = candlesS30.get(c.getIndex()-30).getDate();
					LocalDateTime indexEnd = candlesS30.get(Math.min(c.getIndex()+5, candlesS30.size()-1)).getDate();

					int distance = c.getIndex() - candleBreak.getIndex();
					if (distance > 50) {
						return null;
					}
					Candle cTradeStart = candlesS30.get(c.getIndex()+1);
					Trade trade = Trade.builder()
							.startDate(indexStart)
							.endDate(indexEnd)
							.timeRange(EnumTimeRange.S30)
							.rangeMin(min)
							.rangeMax(max)
							.sl(sl)
							.tp(min + height/2)
							.springUnderPrice(springUnderPrice)
							.springAbovePrice(springAbovePrice)
							.underRangeLimit(min - 0.1 * height)
							.aboveRange1(min + 0.05 * height)
							.aboveRange2(min + 0.1 * height)
							.aboveRange3(min + 0.15 * height)
							.height(height)
							.build();
					addFeature(candlesS30, candleBreak, cTradeStart, trade, range);
					return trade;
				}
				if (c.getHigh() > max) {
					return null;
				}
				if (c.getLow() < min - height*0.3) {
					return null;
				}
			}
		}
		return null;
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
				increaseCount("TP");
				increaseDouble("RESULT", rr);
				break;
			}
			if (c.getLow() <= trade.getSl()) {
				isTP = false;
				endCandle = c;
				increaseCount("SL");
				increaseDouble("RESULT", -1d);
				break;
			}
		}
		if (endCandle == null) {
			return;
		}
		List<HorizontalLine> lines = List.of(
				buildLine(trade.getStartDate(), trade.getRangeMin(), "#FFFFFF"),
				buildLine(trade.getStartDate(), trade.getSl(), "#F54927"),
				buildLine(trade.getStartDate(), trade.getTp(), "#00F53A"),
				buildLine(trade.getStartDate(), trade.getSpringAbovePrice(), "#EBB731"),
				buildLine(trade.getStartDate(), trade.getSpringUnderPrice(), "#EBB731")
				);
		saveHotSpot(trade.getStartDate(),
				endCandle.getDate(), 
				List.of(cTradeStart.getDate(), endCandle.getDate()),
				cBreak.getMarket(),
				EnumTimeRange.S30,
				HOTSPOT_CODE,
				HotSpotData.builder()
				.lines(lines)
				.points(List.of(DatePoint.builder().date(cTradeStart.getDate()).color("#FFFF3D").build()))
				.build());
		double breakSize = (trade.getRangeMin() - trade.getSl())/trade.getHeight();
		Map<String, Object> mapFeature = new HashMap<String, Object>();
		mapFeature.put("timestamp", Utils.getTimestamp(cBreak.getDate()));
		mapFeature.put("f_time_since_break", cTradeStart.getIndex() - cBreak.getIndex());
		mapFeature.put("f_rr", rr);
		mapFeature.put("f_range_width", range.getWidth());
		mapFeature.put("f_range_height", range.getHeight() / range.getCandleMax().getAtr());
		mapFeature.put("f_breakSize", breakSize);
		mapFeature.put("f_timestamp", Utils.getTimestamp(cBreak.getDate()));
		mapFeature.put("y", isTP ? 1d : 0d);
		listMapFeatures.add(mapFeature);
	}


	//	double springConfirmationPrice = min + RATION_SPRING_CONFIRMATION * height;
	//	System.out.println("New trade at " + range.getDateStart());
	//	LocalDateTime indexStart = candlesS30.get(c.getIndex()-30).getDate();
	//	LocalDateTime indexEnd = candlesS30.get(Math.min(c.getIndex()+5, candlesS30.size()-1)).getDate();
	//	return Trade.builder()
	//			.startDate(indexStart)
	//			.endDate(indexEnd)
	//			.timeRange(EnumTimeRange.S30)
	//			.rangeMin(min)
	//			.springBreakPrice(min - c.getAtr())
	//			.underRangeLimit(min - 0.1 * height)
	//			.aboveRange1(min + 0.05 * height)
	//			.aboveRange2(min + 0.1 * height)
	//			.aboveRange3(min + 0.15 * height)
	//			.build();


	//	private List<Spring> processSprings(List<Candle> candles, int startIndex, int endIndex, List<Integer> minList, List<Integer> maxList) {
	//		Integer firstMinIndex = minList.get(0);
	//		Integer firstMaxIndex = maxList.get(0);
	//		if (firstMinIndex > firstMaxIndex) {
	//			return List.of();
	//		}
	//		List<Spring> springs = new ArrayList<>();
	//		double min = Double.MAX_VALUE;
	//		double max = Double.MIN_VALUE;
	//		double height = 0;
	//		Candle firstMax = null;
	//		Spring spring = null;
	//		for (int i = startIndex;  i <= endIndex; i++) {
	//			Candle c = candles.get(i);
	//			height = max - min;
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
	//			if (i <= firstMaxIndex) {
	//				min = Math.min(min, c.getLow());
	//				max = Math.max(max, c.getMax());
	//				height = max - min;
	//			} else	 {
	//				double springConfirmationPrice = min + RATION_SPRING_CONFIRMATION * height;
	//				if (c.getLow() < min) {
	////					System.out.println(c.getDate());
	//					if (spring == null) {
	//						spring = new Spring();
	//						spring.setDateBreak(c.getDate());
	//						spring.setConfirmationPrice(springConfirmationPrice);
	//						spring.setP1(min + 0.1 * height);
	//						spring.setP2(min + 0.05 * height);
	//						spring.setMin(min);
	//						spring.setP3(min - 0.3 * height);
	//						springs.add(spring);
	//					}
	//					if (c.getClose() < min && spring.getIndexBreak() == null) {
	//						spring.setIndexBreak(i);
	//					}
	//				}
	//				if (spring != null && c.getClose() > springConfirmationPrice) {
	//					if (spring.getIndexBreak() == null || (i - spring.getIndexBreak() < 5)) {
	//						spring.setConfirmed(true);
	//						spring = null;
	//					}
	//				}
	//			}
	//
	//		}
	//		return springs;
	//	}


	private Candle getLastExtremum(List<Candle> minList, List<Candle> maxList) {
		List<Candle> extremumList = new ArrayList<>(minList);
		extremumList.addAll(maxList);
		return extremumList.stream()
				.sorted(Comparator.comparingInt(Candle::getIndex).reversed()).findFirst().get();
	}


	private Candle getFirstExtremum(List<Candle> minList, List<Candle> maxList) {
		List<Candle> extremumList = new ArrayList<>(minList);
		extremumList.addAll(maxList);
		return extremumList.stream()
				.sorted(Comparator.comparingInt(Candle::getIndex)).findFirst().get();
	}


	private boolean isRangeValid(List<Candle> minList, List<Candle> maxList, int actualIndex) {
		if (minList.size() < 2 || maxList.size() < 1) {
			return false;
		}
		//		List<Candle> allExtremumsSorted = new ArrayList<>(minList);
		//		allExtremumsSorted.addAll(maxList);
		//		allExtremumsSorted = allExtremumsSorted.stream().sorted(Comparator.comparingInt(c -> c.getIndex())).toList();
		//		Integer firstIndex = allExtremumsSorted.get(0).getIndex();
		//		Integer lastIndex = allExtremumsSorted.get(allExtremumsSorted.size()-1).getIndex();
		//		if (lastIndex - firstIndex > 100) {
		//			return false;
		//		}

		List<Candle> sortedBottomsList = minList.stream().sorted(Comparator.comparingDouble(c -> c.getMin())).toList();
		List<Candle> sortedTopsList = maxList.stream().sorted(Comparator.comparingDouble(Candle::getMax).reversed()).toList();

		Candle maxCandle = sortedTopsList.get(0);
		double max = sortedTopsList.get(0).getMax();
		double min = sortedBottomsList.get(0).getMin();
		double rangeHeight = max - min;
		if (rangeHeight < maxCandle.getAtr() * MIN_RANGE_ATR_RATIO) {
			mapReason.put(actualIndex, "RANGE TOO SMALL");
			return false;
		}
		double middleOfRange = min + rangeHeight/2;
		long nbTopsAboveMiddle = maxList.stream()
				.filter(c -> c.getMax() > middleOfRange)
				.count(); 
		boolean topsAreOk = nbTopsAboveMiddle >= NB_BOTTOMS_REQUIRED - 1;
		//		boolean topsAreOk = sortedTopsList.get(1).getMax() >= topBandLimit;
		//		boolean bottomsAreOk = sortedBottomsList.get(1).getMin() <= bottomBandLimit
		//				&& sortedBottomsList.get(2).getMin() <= bottomBandLimit;
		double bottomBandLimit = min + BAND_RATIO * rangeHeight;
		long nbBottomsUnderBand = sortedBottomsList.stream()
				.filter(c -> c.getMin() <= bottomBandLimit)
				.count();
		boolean bottomsAreOk = nbBottomsUnderBand >= NB_BOTTOMS_REQUIRED;
		if (!bottomsAreOk) {
			mapReason.put(actualIndex, "BOTTOMS NOT OK");
		}
		if (!topsAreOk) {
			mapReason.put(actualIndex, "TOPS NOT OK");
		}
		return  bottomsAreOk && topsAreOk;
	}


}


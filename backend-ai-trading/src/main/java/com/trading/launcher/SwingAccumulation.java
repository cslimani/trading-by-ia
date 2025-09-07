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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
import com.trading.service.Range;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile("swing-accumulation")
@Slf4j
public class SwingAccumulation extends AbstractService implements CommandLineRunner {

	private static final int MIN_RANGE_WIDTH = 30;
	private static final Double MAX_RANGE_ATR_RATIO = 4.5d;
	private static final Double MIN_RANGE_ATR_RATIO = 1d;
	private static final Double RATIO_MAX_RANGE_SWING_HIGH = 0.7;
	private static final String HOTSPOT_CODE = "RANGE_AUTO";
	private static final double BAND_RATIO = 0.2d;
	private static final double MIN_MAX_ATR_RATIO = 2d;
	private static final Integer NB_CANDLES_BEFORE = 70;
	private static final double BREAK_ATR_RATIO = 0.5;
	private static final double NB_BOTTOMS_REQUIRED = 2;

	Map<Integer, String> mapReason = new ConcurrentHashMap<>();
	@Autowired
	FeatureWriterSpring featureWriter;
	SwingExtremaFinder analyzer = new SwingExtremaFinder();

	EnumTimeRange timeRange = EnumTimeRange.M5;

	ExecutorService executor = Executors.newFixedThreadPool(15);


	@Override
	@Transactional
	public void run(String... args) throws Exception {
		System.out.println("Accumulation Break starting");
		backupHotSpots();
		hotSpotRepository.deleteByCode(HOTSPOT_CODE);
		List<HotSpot> hotSpotsToFind = new CopyOnWriteArrayList<HotSpot>();
		long startProcess = System.currentTimeMillis();
		Map<String, LocalDateTime> bigMap = Map.ofEntries(
//				entry("US100",  LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0))
				entry("GOLD",   LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0))
//				entry("SILVER",   LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("EURUSD", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("BRENT",  LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("COPPER", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("USDJPY", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("DAX30",  LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("ETHUSD", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("BTCUSD", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("GBPUSD", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("US500", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("US30", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("AUDUSD", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("USDCHF", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("WTI", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)),
//				entry("CHINA", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0))
				);
		bigMap.forEach((market, startDate) -> {

			LocalDateTime endDate = LocalDateTime.of(2025, Month.DECEMBER, 1, 0, 0);

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
		featureWriter.writeAll();
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
		List<LocalDateTime> swingHighList = new ArrayList<>();
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
				if (!newRange.getBreakFromTop()) {
					continue;
				}
				if (swingHighList.contains(newRange.getDateStart())) {
					continue;
				}

				increaseCount("RANGE_OK");
				//				System.out.println("New Range ok with end date" + newRange.getDateEnd());
				checkHotSpotFound(newRange, candles, hotSpotsToFind);
				saveHotSpot(newRange.getDateStart(),
						newRange.getDateEnd(), 
						List.of(newRange.getDateStart(), newRange.getDateEnd()),
						market,
						timeRange,
						HOTSPOT_CODE,
						null);


				swingHighList.add(newRange.getDateStart());
			} else {
			}
		}
		checkHotSpotNotFound(candles, hotSpotsToFind);
		return hotSpotsToFind;
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
			//			if (isRangeValid(minList, maxList, BAND_RATIO)) {
			//				return buildRange(candles, actualIndex, min, max, minList, maxList, extremumsReversed);
			//			}
			//			System.out.println(e.getDate());

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
						if (Math.abs(maxTop.getMax() - c.getMax()) < BREAK_ATR_RATIO *c.getAtr()) {
							rescueTop = true;
							c.setRescued(true);
						}
						if (!rescueTop) {
							debug(c.getDate() + " new max out of bounds");
							if (isRangeValid(minList, maxList, BAND_RATIO, actualIndex)) {
								debug(c.getDate() + " return valid range");
								return buildRange(candles, actualIndex, min, max, minList, maxList, extremumsReversed);
							} else {
								return null;
							}
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
						if (Math.abs(minBottom.getMin() - c.getMin()) < BREAK_ATR_RATIO *c.getAtr()) {
							rescueBottom = true;
							c.setRescued(true);
						}
						if (!rescueBottom) {
							mapReason.put(actualIndex, "BREAK FROM BOTTOM");
							debug(c.getDate() + " new min out of bounds");
							if (isRangeValid(minList, maxList, BAND_RATIO, actualIndex)) {
								return null;
								//						return buildRange(candles, endIndex, min, max, minList, maxList, extremumsReversed);
							} else {
								return null;
							}
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
			List<Candle> minList, List<Candle> maxList, List<Extremum> extremumsReversed) {
		Candle firstCandle = getFirstExtremum(minList, maxList);
		Candle lastCandle = getLastExtremum(minList, maxList);
		int endIndex = -1;
		Integer firstIndex = firstCandle.getIndex();
		Boolean breakFromTop = null;
		//First index
		int startIndex = -1;
		for (int i = 1; i < firstIndex; i++) {
			Candle cTmp = candles.get(firstIndex - i);
			if (cTmp.getMin() < min - BREAK_ATR_RATIO * cTmp.getAtr()) {
				mapReason.put(actualIndex, "START FROM BOTTOM");
				return null;
			}
			if (cTmp.getMax() > max + BREAK_ATR_RATIO * cTmp.getAtr()) {
				startIndex = firstIndex -i +1;
				break;
			}
		}

		//Last index
		Integer lastIndex = lastCandle.getIndex();
		for (int i = 1; lastIndex + i < candles.size(); i++) {
			Candle cTmp = candles.get(lastIndex + i);
			if (cTmp.getMin() < min - BREAK_ATR_RATIO * cTmp.getAtr()) {
				endIndex = lastIndex + i -1;
				breakFromTop = false;
				break;
			}
			if (cTmp.getMax() > max + BREAK_ATR_RATIO * cTmp.getAtr()) {
				endIndex = lastIndex + i -1;
				breakFromTop = true;
				break;
			}
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
				.breakFromTop(breakFromTop)
				.min(min)
				.max(max)
				.build();
	}


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


	private boolean isRangeValid(List<Candle> minList, List<Candle> maxList, double ratioBand, int actualIndex) {
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
		double bottomBandLimit = min + ratioBand * rangeHeight;
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

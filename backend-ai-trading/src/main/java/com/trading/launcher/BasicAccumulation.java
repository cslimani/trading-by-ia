package com.trading.launcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.trading.entity.Candle;
import com.trading.enums.EnumTimeRange;
import com.trading.indicator.AtrCalculator;
import com.trading.indicator.MedianCalculator;
import com.trading.indicator.extremum.Extremum;
import com.trading.indicator.extremum.SwingExtremaFinder;
import com.trading.indicator.extremum.SwingExtremaFinder.Type;
import com.trading.service.AbstractService;
import com.trading.service.FeatureWriterSpring;
import com.trading.service.Range;

import lombok.extern.slf4j.Slf4j;

@Component
@Profile("basic-accumulation")
@Slf4j
public class BasicAccumulation extends AbstractService implements CommandLineRunner {

	private static final int MIN_RANGE_WIDTH = 40;
	private static final Double MAX_RANGE_ATR_RATIO = 4d;
	private static final Double RATIO_MAX_RANGE_SWING_HIGH = 0.7;
	private static final String HOTSPOT_CODE = "RANGE_AUTO";

	@Autowired
	FeatureWriterSpring featureWriter;
	SwingExtremaFinder analyzer = new SwingExtremaFinder();

	EnumTimeRange timeRange = EnumTimeRange.M5;

	ExecutorService executor = Executors.newFixedThreadPool(15);

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Accumulation Break starting");
		featureWriter.backupFile();
		hotSpotRepository.deleteAll();
		long startProcess = System.currentTimeMillis();
		Map.of(
//				"GOLD", LocalDateTime.of(2015, Month.JANUARY, 1, 0, 0)
				"US100", LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0)
//				,"EURUSD", LocalDateTime.of(2015, Month.JANUARY, 1, 0, 0)
				)
		.forEach((market, startDate) -> {

			LocalDateTime endDate = LocalDateTime.of(2025, Month.DECEMBER, 1, 0, 0);

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
		featureWriter.writeAll();
		long endProcess = System.currentTimeMillis();
		System.out.println("Total time processing " + (endProcess - startProcess)/1000 + " sec");
	}


	private void process(String market, LocalDateTime startDate, LocalDateTime endDate) throws IOException {
		System.out.println("Start processing " + startDate + " for " + market);
		List<Candle> candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(
				market,
				timeRange,
				startDate,
				endDate);
		setIndex(candles);
		AtrCalculator.compute(candles, 50);
		List<Extremum> extremums7 = analyzer.findExtrema(candles, 7, false);
		List<Extremum> extremumsEntry = analyzer.findExtrema(candles, 2, false);
		List<Integer> swingHighList = new ArrayList<>();
		Range range = null;
		for (int i = 100; i < candles.size() - 100; i++) {
			Candle c = candles.get(i);
			if (range == null) {
				//				Double averageATR = AtrCalculator.average(candles, i, 50);
				Double maxRangeHeight = MAX_RANGE_ATR_RATIO*c.getAtr();
				if (isInvalid(List.of(maxRangeHeight, c.getAtr()))) {
					continue;
				}

				Range newRange = getLargestRangeFast(candles, i, maxRangeHeight);
				if (!isRangeValid(newRange, candles)) {
					continue;
				}

				Candle firstAccumulationCandle = candles.get(newRange.getIndexStart());
				Optional<Extremum> extremumBefore = getFirstExtremumBefore(firstAccumulationCandle, extremums7, Type.MAX);
				if (!extremumBefore.isPresent()) {
					continue;
				}
				Candle swingHighBefore = extremumBefore.get().getCandle();
				newRange.setSwingHighBefore(swingHighBefore);
				newRange.setFirstAccumulationCandle(firstAccumulationCandle);
				if (swingHighList.contains(swingHighBefore.getIndex())) {
					//					System.out.println("Swing high already met at " + swingHighBefore.getIndex());
					continue;
				}

				double rangeSwingHighRatio = getRangeSwingHighRatio(newRange);
				if (rangeSwingHighRatio > RATIO_MAX_RANGE_SWING_HIGH) {
					continue;
				}
				newRange.setRangeSwingHighRatio(rangeSwingHighRatio);

				range = newRange;
				swingHighList.add(swingHighBefore.getIndex());

				//				increaseCount("TOTAL");
				int rangeWidth = range.indexEnd - range.indexStart;
				//				System.out.println();
				//				System.out.println("rangeWidth " + rangeWidth);
				//				System.out.println("rangeStart " + candles.get(range.indexStart).getDate());
				//				System.out.println("rangeEnd " + candles.get(range.indexEnd).getDate());
				//				System.out.println();
			} else {
				LocalDateTime dateSwingHighBefore = range.getSwingHighBefore().getDate();
				Candle firstAccumulationCandle =  range.getFirstAccumulationCandle();
				List<LocalDateTime> keyDates = List.of(dateSwingHighBefore, firstAccumulationCandle.getDate(),c.getDate());
				if (c.getClose() > range.getMax()) {
					//					if(range.getNbBreakUp().getAndIncrement() > 0) {
					//						System.out.println("Break from top at " + c.getDate());
					increaseCount("BREAK UP");
					//					saveHotSpot(dateSwingHighBefore, c.getDate(), keyDates, market, timeRange, HOTSPOT_CODE);
					if (isCondition(candles, range, c, extremumsEntry)) {
						increaseCount("CONDITIONS OK");
						//						saveHotSpot(swingHighBefore, c.getDate(), keyDates, market, timeRange, HOTSPOT_CODE);
						featureWriter.addFeature(candles, range, c, extremumsEntry, market, timeRange, HOTSPOT_CODE);
					}

					range = null;
					//						if (indexEnd > 0) {
					//							saveHotSpot(swingHighBefore, candles.get(indexEnd).getDate(), keyDates, market, timeRange, HOTSPOT_CODE);
					//							//						continueTrade(candles, range, c, extremums2);
					//						}
					//					}
				} else if (c.getClose() < range.getMin()) {
					//					increaseCount("BREAK DOWN");
					if(range.getNbBreakDown().getAndIncrement() > 0) {
						//						saveHotSpot(swingHighBefore, c.getDate(), keyDates, market, timeRange, "RANGE_AUTO");
						//						System.out.println("Break from bottom at" + c.getDate());
						range = null;
					}
				} else {
					//					extendRange(range, c);
				}
			}
		}

	}


	private boolean isCondition(List<Candle> candles, Range range, Candle c, List<Extremum> extremumsEntry) {
//		if(c.getDate().getHour() < 9 && c.getDate().getHour() >= 20) {
//			return false;
//		}
//		if (!atLeastRetestBottom(candles, range, c)) {
//			return false;
//		}
		//		if (!atLeastSpring(candles, range, c)) {
		//			return false;
		//		}
		return true;
	}


	private boolean atLeastSpring(List<Candle> candles, Range range, Candle candleBreak) {
		Candle accStart = range.getFirstAccumulationCandle();
		int midIndex = (candleBreak.getIndex() + accStart.getIndex())/2;
		Candle springCandle = getSpringCandle(candles, range, candleBreak);
		return springCandle.getIndex() > midIndex;
	}



	private boolean atLeastRetestBottom(List<Candle> candles, Range range, Candle candleBreak) {
		Candle accStart = range.getFirstAccumulationCandle();
		int midIndex = (candleBreak.getIndex() + accStart.getIndex())/2;
		return candles.stream()
				.filter(c -> c.getIndex() > midIndex && c.getIndex() < candleBreak.getIndex())
				.anyMatch(c -> c.getLow() < range.getLow());
	}


	private void continueTrade(List<Candle> candles, Range range, Candle candleBreak, List<Extremum> extremums2) {
		double tp = range.getMax() + range.getHeight()*1.5;
		Optional<Extremum> extremumBefore = getFirstExtremumBefore(candleBreak, extremums2, Type.MIN);
		if (!extremumBefore.isPresent()) {
			return;
		}
		double sl = extremumBefore.get().getCandle().getLow();
		double startTradePrice = candles.get(candleBreak.getIndex()+1).getOpen();
		double rr = (tp - startTradePrice) / (startTradePrice - sl);
		if (rr < 1) {
			return;
		}
		for (int i=  candleBreak.getIndex()+1; i< candles.size(); i++) {
			Candle c = candles.get(i);
			if (c.getHigh() >= tp) {
				increaseCount("TP1");
				System.out.println("Won " + rr + "R");
				increaseDouble("TOTAL_WON", rr);
				break;
			}
			if (c.getLow() <= sl) {
				increaseCount("SL");
				System.out.println("Lost -1*R");
				increaseDouble("TOTAL_WON", -1d);
				break;
			}
		}
	}


	private double getRangeSwingHighRatio(Range range) {
		Double priceSwingHigh = range.getSwingHighBefore().getHigh();
		Double priceRangeTop = range.getMax();
		Double priceRangeBottom = range.getMin();
		Double rangeHeight =  priceRangeTop - priceRangeBottom;
		range.setMaxHeight(rangeHeight);
		return rangeHeight / (priceSwingHigh - priceRangeBottom);
	}


	//	private void extendRange(Range range, Candle c) {
	//		range.setDateEnd(c.getDate());
	//		range.setMin(Math.min(range.getMin(), c.getLow()));
	//		range.setMax(Math.max(range.getMax(), c.getHigh()));
	//		range.setIndexEnd(c.getIndex());
	//	}


	private boolean isRangeValid(Range range, List<Candle> candles) {
		if (range == null) {
			return false;
		}
		int rangeWidth = range.indexEnd - range.indexStart;
		if (rangeWidth < MIN_RANGE_WIDTH) {
			return false;
		}
		for(int i =0; i < range.indexStart; i++) {
			Candle cBefore= candles.get(range.indexStart - i);
			if (cBefore.getHigh() > range.high) {
				return true;
			} else if (cBefore.getLow() < range.low) {
				return false;
			} 
		}
		return false;
	}

	private Range getLargestRangeSlow(List<Candle> candles, int endIndex, Double maxRangeHeight) {
		Range range = null;
		for (int i = 1; i < endIndex; i++) {
			//			if (candles.get(endIndex).isDate(3, 1) && candles.get(endIndex).isTime(0, 40, 0)) {
			//				System.out.println();
			//			}
			int startIndex = endIndex - i;
			List<Candle> subList = new ArrayList<Candle>(candles.subList(startIndex, endIndex+1));
			PercentileResult candleHigh = percentile(subList, 95d, true);
			PercentileResult candleLow = percentile(subList, 5d, false);
			double rangeHeight = Math.abs(candleHigh.value - candleLow.value);
			if (rangeHeight > maxRangeHeight) {
				Range rangeFast = getLargestRangeFast(candles, endIndex, maxRangeHeight);
				if (range != null 
						&& rangeFast != null 
						&& (!rangeFast.getIndexStart().equals(range.getIndexStart())
								|| !rangeFast.getIndexEnd().equals(range.getIndexEnd())
								||  !rangeFast.getDateStart().equals(range.getDateStart()))) {
					System.out.println("Slow " + range.getDateStart() + " " + range.getDateEnd());
					System.out.println("Fast " + rangeFast.getDateStart() + " " + rangeFast.getDateEnd());
				}
				return range;
			}

			removeLeadingAboveMedian(subList);
			if (subList.isEmpty()) {
				//				increaseCount("EMPTY_AFTER_REMOVE_LEADING");
				return null;
			}

			Double maxRange = subList.stream().map(c -> c.getHigh()).max(Double::compareTo).get();
			Double minRange = subList.stream().map(c -> c.getLow()).min(Double::compareTo).get();
			range = new Range(
					candles.get(startIndex).getDate(),
					candles.get(endIndex).getDate(),
					startIndex,
					endIndex, 
					candleHigh.value, 
					candleLow.value, 
					rangeHeight,
					maxRange, 
					minRange);
		}
		return range;
	}

	private Range getLargestRangeFast(List<Candle> candles, int endIndex, Double maxRangeHeight) {
		Range range = null;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;

		for (int i = 0; i < endIndex; i++) {
			int startIndex = endIndex - i;
			Candle candle = candles.get(startIndex);
			if (candle.getLow() < min) {
				min = candle.getLow();
			}
			if (candle.getHigh() > max) {
				max = candle.getHigh();
			}
			double rangeHeight = max - min;
			if (rangeHeight <= maxRangeHeight ) {
				range = new Range(
						candles.get(startIndex).getDate(),
						candles.get(endIndex).getDate(),
						startIndex,
						endIndex, 
						max, 
						min, 
						rangeHeight,
						max, 
						min);
			} else {
				List<Candle> subList = new ArrayList<Candle>(candles.subList(startIndex, endIndex+1));
				PercentileResult candleHigh =  percentile(subList, 95d, true);
				PercentileResult candleLow = percentile(subList, 5d, false);
				rangeHeight = Math.abs(candleHigh.value - candleLow.value);
				if (rangeHeight <= maxRangeHeight) {
					//					Double maxRange = subList.stream().map(c -> c.getHigh()).max(Double::compareTo).get();
					//					Double minRange = subList.stream().map(c -> c.getLow()).min(Double::compareTo).get();
					range = new Range(
							candles.get(startIndex).getDate(),
							candles.get(endIndex).getDate(),
							startIndex,
							endIndex, 
							candleHigh.value, 
							candleLow.value, 
							rangeHeight,
							-1d, 
							-1d);
				} else if (range != null && range.size() > 10) {
					int indexShift = removeLeadingAboveMedian(subList);
					int newStartIndex = range.indexStart + indexShift;
					if (newStartIndex >= range.indexEnd) {
						return null;
					}
					subList = new  ArrayList<Candle>(candles.subList(newStartIndex, range.indexEnd + 1));
					Double maxRange = getMax(subList);
					Double minRange = getMin(subList);
					double newRangeHeight = maxRange - minRange;
					range = new Range(
							candles.get(newStartIndex).getDate(),
							candles.get(endIndex).getDate(),
							newStartIndex,
							endIndex, 
							range.high, 
							range.low, 
							newRangeHeight,
							maxRange, 
							minRange);
					return range;
				} else {
					return null;
				}
			}
		}
		return range;
	}

	public double getMax(List<Candle> subList){
		double maxRange = Double.MIN_VALUE;
		for (Candle c : subList) {
			if (c.getHigh() > maxRange) {
				maxRange = c.getHigh();
			}
		}
		return maxRange;
	}

	public double getMin(List<Candle> subList){
		double minRange = Double.MAX_VALUE;
		for (Candle c : subList) {
			if (c.getLow() < minRange) {
				minRange = c.getLow();
			}
		}
		return minRange;
	}

	public static int removeLeadingAboveMedian(List<Candle> candles) {
		double median = MedianCalculator.median(candles);
		int index = 0;
		while (index < candles.size() && candles.get(index).close > median) {
			index++;
		}
		return index;
	}


	public static class PercentileResult {
		public final double value;
		public final int index;

		public PercentileResult(double value, int index) {
			this.value = value;
			this.index = index;
		}
	}

	public static PercentileResult percentile(List<Candle> candles, double p, boolean high) {
		if (Double.isNaN(p) || p < 1 || p > 100.0)
			throw new IllegalArgumentException("Le centile doit être entre 0 et 100.");

		List<double[]> hiIdx = new ArrayList<>(candles.size());
		for (int i = 0; i < candles.size(); i++) {
			Candle c = candles.get(i);
			if (high) {
				hiIdx.add(new double[]{c.high, c.getIndex()});
			} else {
				hiIdx.add(new double[]{c.low, c.getIndex()});
			}
		}
		if (hiIdx.isEmpty()) throw new IllegalArgumentException("Aucune valeur 'high' valide.");

		// Tri croissant par high
		hiIdx.sort(Comparator.comparingDouble(a -> a[0]));

		int n = hiIdx.size();
		int rank = (int) Math.ceil((p / 100.0) * n) - 1;
		if (rank < 0) rank = 0;
		if (rank >= n) rank = n - 1;

		double value = hiIdx.get(rank)[0];
		int indexInOriginal = (int) hiIdx.get(rank)[1];

		return new PercentileResult(value, indexInOriginal);
	}

}

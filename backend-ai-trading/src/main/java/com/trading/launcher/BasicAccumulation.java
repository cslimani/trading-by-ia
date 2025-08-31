package com.trading.launcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import com.trading.service.FeatureWriter2;
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
	FeatureWriter2 featureWriter;
	SwingExtremaFinder analyzer = new SwingExtremaFinder();

	EnumTimeRange timeRange = EnumTimeRange.M1;

	ExecutorService executor = Executors.newFixedThreadPool(15);

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Accumulation Break starting");
		featureWriter.backupFile();
		hotSpotRepository.deleteAll();

//		List.of("GOLD", "US100", "DAX30")
		List.of("DAX30")
		.forEach(market -> {
			LocalDateTime startDate = LocalDateTime.of(2018, Month.JANUARY, 1, 0, 0);
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
		List<Extremum> extremums3 = analyzer.findExtrema(candles, 3, false);
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
				Range newRange = getLargestRange(candles, i, maxRangeHeight);
				if (!isRangeValid(newRange, candles)) {
					continue;
				}

				Candle firstAccumulationCandle = candles.get(newRange.getIndexStart());
				Candle swingHighBefore = getFirstExtremumBefore(firstAccumulationCandle, extremums7, Type.MAX);
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

				increaseCount("TOTAL");
				int rangeWidth = range.indexEnd - range.indexStart;
				//				System.out.println();
				//				System.out.println("rangeWidth " + rangeWidth);
				//				System.out.println("rangeStart " + candles.get(range.indexStart).getDate());
				//				System.out.println("rangeEnd " + candles.get(range.indexEnd).getDate());
				//				System.out.println();
			} else {
				LocalDateTime swingHighBefore = range.getSwingHighBefore().getDate();
				Candle firstAccumulationCandle =  range.getFirstAccumulationCandle();
				List<LocalDateTime> keyDates = List.of(swingHighBefore, firstAccumulationCandle.getDate(),c.getDate());
				if (c.getClose() > range.getMax()) {
					//					if(range.getNbBreakUp().getAndIncrement() > 0) {
					//						System.out.println("Break from top at " + c.getDate());
					increaseCount("BREAK UP");
					Integer indexEnd = featureWriter.addFeature(candles, range, c, extremums3, market, timeRange, HOTSPOT_CODE);
					range = null;
					//						if (indexEnd > 0) {
					//							saveHotSpot(swingHighBefore, candles.get(indexEnd).getDate(), keyDates, market, timeRange, HOTSPOT_CODE);
					//							//						continueTrade(candles, range, c, extremums2);
					//						}
					//					}
				} else if (c.getClose() < range.getMin()) {
					increaseCount("BREAK DOWN");
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


	private void continueTrade(List<Candle> candles, Range range, Candle candleBreak, List<Extremum> extremums2) {
		double tp = range.getMax() + range.getHeight()*1.5;
		double sl = getFirstExtremumBefore(candleBreak, extremums2, Type.MIN).getLow();
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
				increaseCount("TOTAL_WON", rr);
				break;
			}
			if (c.getLow() <= sl) {
				increaseCount("SL");
				System.out.println("Lost -1*R");
				increaseCount("TOTAL_WON", -1d);
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
		for(int i =0; i<candles.size(); i++) {
			Candle cBefore= candles.get(range.indexStart - i);
			if (cBefore.getHigh() > range.high) {
				return true;
			} else if (cBefore.getLow() < range.low) {
				return false;
			} 
		}
		return false;
	}

	private Range getLargestRange(List<Candle> candles, int endIndex, Double maxRangeHeight) {
		Range range = null;
		for (int i = 1; i < endIndex; i++) {
			//			if (candles.get(endIndex).isDate(3, 1) && candles.get(endIndex).isTime(0, 40, 0)) {
			//				System.out.println();
			//			}
			List<Candle> subList = new ArrayList<Candle>(candles.subList(endIndex-i, endIndex+1));
			PercentileResult candleHigh = percentile(subList, 95d, true);
			PercentileResult candleLow = percentile(subList, 5d, false);
			double rangeHeight = Math.abs(candleHigh.value - candleLow.value);
			if (rangeHeight > maxRangeHeight) {
				return range;
			}

			removeLeadingAboveMedian(subList);
			if (subList.isEmpty()) {
				//				increaseCount("EMPTY_AFTER_REMOVE_LEADING");
				return null;
			}
			int startIndex = endIndex - subList.size() +1;
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

	public static void removeLeadingAboveMedian(List<Candle> candles) {
		double median = MedianCalculator.median(candles);
		int index = 0;
		while (index < candles.size() && candles.get(index).close > median) {
			index++;
		}
		if (index > 0) {
			candles.subList(0, index).clear();
		}
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

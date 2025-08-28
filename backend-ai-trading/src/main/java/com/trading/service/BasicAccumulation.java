package com.trading.service;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

@Component
@Profile("basic-accumulation")
public class BasicAccumulation extends AbstractService implements CommandLineRunner {

	private static final int MIN_RANGE_WIDTH = 40;
	private static final Double MAX_RANGE_ATR_RATIO = 4d;
	private static final Double RATIO_MAX_RANGE_SWING_HIGH = 0.7;
	private static final String HOTSPOT_CODE = "RANGE_AUTO";
	@Autowired
	FeatureWriter featureWriter;
	SwingExtremaFinder analyzer = new SwingExtremaFinder();
	List<Integer> swingHighList = new ArrayList<Integer>();

	LocalDateTime startDate = LocalDateTime.of(2024, Month.JANUARY, 1, 0, 0);
	LocalDateTime endDate = LocalDateTime.of(2026, Month.DECEMBER, 1, 0, 0);
	String market = "US100.cash";
	EnumTimeRange timeRange = EnumTimeRange.M1;

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Impulse Detection starting");
		featureWriter.backupFile();

		hotSpotRepository.deleteAll();
		List<Candle> candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(
				market,
				timeRange,
				startDate,
				endDate);
		setIndex(candles);
		AtrCalculator.compute(candles, 10);
		List<Extremum> extremums7 = analyzer.findExtrema(candles, 7, false);
		List<Extremum> extremums2 = analyzer.findExtrema(candles, 2, false);

		Range range = null;
		for (int i = 100; i < candles.size() - 100; i++) {
			Candle c = candles.get(i);
			if (range == null) {
				Double averageATR = AtrCalculator.average(candles, i, 50);
				Double maxRangeHeight = MAX_RANGE_ATR_RATIO*averageATR;
				if (isInvalid(List.of(maxRangeHeight, averageATR))) {
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
				newRange.setRangeSwingHighRatio(maxRangeHeight);

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
					if(range.getNbBreakUp().getAndIncrement() > 0) {
						//						System.out.println("Break from top at " + c.getDate());
						Integer indexEnd = featureWriter.addFeature(candles, range, c, extremums2, market, timeRange, HOTSPOT_CODE);
						range = null;
						if (indexEnd > 0) {
							saveHotSpot(swingHighBefore, candles.get(indexEnd).getDate(), keyDates, market, timeRange, HOTSPOT_CODE);
							//						continueTrade(candles, range, c, extremums2);
							increaseCount("BREAK UP");
						}
					}
				} else if (c.getClose() < range.getMin()) {
					if(range.getNbBreakDown().getAndIncrement() > 0) {
						//						saveHotSpot(swingHighBefore, c.getDate(), keyDates, market, timeRange, "RANGE_AUTO");
						//						System.out.println("Break from bottom at" + c.getDate());
						increaseCount("BREAK DOWN");
						range = null;
					}
				} else {
					//					extendRange(range, c);
				}
			}
		}
		displayMapCount();
		featureWriter.writeAll();
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


	private boolean isInvalid(List<Double> list) {
		return list.stream()
				.anyMatch(d -> Double.isInfinite(d) || Double.isNaN(d));
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

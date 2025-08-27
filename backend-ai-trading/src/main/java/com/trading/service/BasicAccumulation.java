package com.trading.service;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@Profile("basic-accumulation")
public class BasicAccumulation extends AbstractService implements CommandLineRunner {

	private static final int EXTREMUM_PARAM = 5;
	private static final double ALPHA_BREAK = 0.3;
	private static final double TETA_BREAK = 0.3;

	SwingExtremaFinder analyzer = new SwingExtremaFinder();
	List<Integer> swingHighList = new ArrayList<Integer>();
	
	LocalDateTime startDate = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);
	LocalDateTime endDate = LocalDateTime.of(2026, Month.DECEMBER, 1, 0, 0);
	String market = "US100.cash";
	EnumTimeRange timeRange = EnumTimeRange.M1;

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Impulse Detection starting");
		hotSpotRepository.deleteAll();
		List<Candle> candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDate(
				market,
				timeRange,
				startDate,
				endDate);
		setIndex(candles);
		AtrCalculator.compute(candles, 10);
		List<Extremum> extremums7 = analyzer.findExtrema(candles, 7, false);
		
		Range range = null;
		for (int i = 100; i < candles.size() - 100; i++) {
			Candle c = candles.get(i);
			Double averageATR = AtrCalculator.average(candles, i, 100);
			Double maxRangeHeight = 4*averageATR;
			if (isInvalid(List.of(maxRangeHeight, averageATR))) {
				continue;
			}
			if (range == null) {
				Range newRange = getLargestRange(candles, i, maxRangeHeight);
				if (!isRangeValid(newRange, candles)) {
					continue;
				}
				
				Candle firstAccumulationCandle = candles.get(newRange.getIndexStart());
				Candle swingHighBefore = getFirstExtremumBefore(firstAccumulationCandle, extremums7, Type.MAX);
				newRange.setSwingHighBefore(swingHighBefore);
				newRange.setFirstAccumulationCandle(firstAccumulationCandle);
				if (swingHighList.contains(swingHighBefore.getIndex())) {
					System.out.println("Swing high already met at " + swingHighBefore.getIndex());
					continue;
				}
				
				if (!isPreviousSwingHighValid(newRange)) {
					continue;
				}
				
				
				range = newRange;
				swingHighList.add(swingHighBefore.getIndex());
				
				int rangeWidth = range.indexEnd - range.indexStart;
				increaseCount("RANGE_AUTO");
				System.out.println();
				System.out.println("rangeWidth " + rangeWidth);
				System.out.println("rangeStart " + candles.get(range.indexStart).getDate());
				System.out.println("rangeEnd " + candles.get(range.indexEnd).getDate());
				System.out.println();
			} else {
				LocalDateTime swingHighBefore = range.getSwingHighBefore().getDate();
				Candle firstAccumulationCandle =  range.getFirstAccumulationCandle();
				List<LocalDateTime> keyDates = List.of(swingHighBefore, firstAccumulationCandle.getDate());
				if (c.getClose() > range.getMax()) {
					System.out.println("Break from top at " + c.getDate());
					saveHotSpot(swingHighBefore, c.getDate(), keyDates, market, timeRange, "RANGE_AUTO");
					range = null;
				} else if (c.getClose() < range.getMin()) {
					saveHotSpot(swingHighBefore, c.getDate(), keyDates, market, timeRange, "RANGE_AUTO");
					System.out.println("Break from bottom at" + c.getDate());
					range = null;
				} else {
					extendRange(range);
				}
			}
		}
		displayMapCount();
	}


	private boolean isPreviousSwingHighValid(Range range) {
		Double priceSwingHigh = range.getSwingHighBefore().getHigh();
		Double priceRangeTop = range.getMax();
		Double priceRangeBottom = range.getMin();
		Double rangeHeight =  priceRangeTop - priceRangeBottom;
		return priceSwingHigh >= priceRangeBottom + 1.5*rangeHeight;
	}


	private void extendRange(Range range) {
		// TODO Auto-generated method stub
		
	}


	private boolean isRangeValid(Range range, List<Candle> candles) {
		if (range == null) {
			return false;
		}
		int rangeWidth = range.indexEnd - range.indexStart;
		if (rangeWidth < 50) {
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
			range = new Range(startIndex, endIndex, candleHigh.value, candleLow.value, rangeHeight,maxRange, minRange);
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
	 
	@Data
	@RequiredArgsConstructor
	public static class Range {

		public boolean isSame(Range newRange) {
			return newRange.getHeight() == height && newRange.getIndexStart() == indexStart;
		}
		@NonNull
		public Integer indexStart;
		@NonNull
		public Integer indexEnd;
		@NonNull
		public Double high;
		@NonNull
		public Double low;
		@NonNull
		public Double height;
		@NonNull
		public Double max;
		@NonNull
		public Double min;
		Candle swingHighBefore;
		Candle firstAccumulationCandle;
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

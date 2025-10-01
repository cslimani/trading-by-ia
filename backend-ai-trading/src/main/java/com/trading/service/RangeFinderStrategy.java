package com.trading.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.trading.dto.Range;
import com.trading.entity.Candle;
import com.trading.enums.ExtremumType;
import com.trading.indicator.MedianCalculator;
import com.trading.indicator.extremum.Extremum;
import com.trading.strategy.AbstractStrategy;

public class RangeFinderStrategy extends AbstractStrategy {

	public static final Double MAX_RANGE_ATR_RATIO_LOW = 5d;
	public static final Double MAX_RANGE_ATR_RATIO_HIGH = 6d;
	public static final Double MIN_RANGE_ATR_RATIO = 1d;
	public static final double BOTTOM_BAND_RATIO = 0.3d;
	public static final double TOP_BAND_RATIO = 0.5d;
	public static final double BREAK_DOWN_ATR_RATIO = 1;
	public static final double BREAK_UP_ATR_RATIO = 0.5;
	public static final double NB_BOTTOMS_REQUIRED = 2;
	public static final int MIN_RANGE_WIDTH = 25;
	public static final Integer NB_CANDLES_BEFORE = 30;
	public static final int MAX_RANGE_WIDTH = 100;
	private static final Double RSI_LIMIT = 50d;

	private boolean isPrevalidated(List<Candle> minList, List<Candle> maxList, Candle currentCandle) {
		if (minList.size() < 2 || maxList.size() < 2) {
			return false;
		}

		List<Candle> sortedBottomsList = minList.stream().sorted(Comparator.comparingDouble(c -> c.getMin())).toList();
		List<Candle> sortedTopsList = maxList.stream().sorted(Comparator.comparingDouble(Candle::getMax).reversed()).toList();

		Candle maxCandle = sortedTopsList.get(0);
		double max = sortedTopsList.get(0).getMax();
		double min = sortedBottomsList.get(0).getMin();
		double rangeHeight = max - min;
		if (rangeHeight < maxCandle.getAtr() * MIN_RANGE_ATR_RATIO) {
			DebugHolder.eliminated("Range height is too high");
			return false;
		}

		// tops validation
		double topBandLimit = max - rangeHeight*TOP_BAND_RATIO;
		long nbTopsInsideBand = maxList.stream()
				.filter(c -> c.getMax() > topBandLimit)
				.count(); 
		boolean topsAreOk = nbTopsInsideBand >= NB_BOTTOMS_REQUIRED;
		if (!topsAreOk) {
			DebugHolder.eliminated("Tops are not valid");
			return false;
		}
		// At least one very top between bottoms
		double middle = min + rangeHeight/2;
		List<Candle> topsAboveMiddle = sortedTopsList.stream()
				.filter(top -> top.getMax() > middle).toList();
		List<Candle> bottomsUnderMiddle = sortedBottomsList.stream()
				.filter(top -> top.getMin() < middle).toList();
		boolean isTopsRepartitionOK = existsBetween(topsAboveMiddle, bottomsUnderMiddle);
		//				.anyMatch(c -> isBetweenIndexes(c.getIndex(), sortedBottomsList.get(0).getIndex(), sortedBottomsList.get(1).getIndex()));
		if (!isTopsRepartitionOK) {
			DebugHolder.eliminated("Tops repartition is not OK");
			return false;
		}
		//		if (sortedBottomsList.size() >= 2 && sortedTopsList.size() >=2) {
		//			boolean isOK1 = ;
		//			boolean isOK2 = isBetweenIndexes(sortedTopsList.get(1).getIndex(), sortedBottomsList.get(0).getIndex(), sortedBottomsList.get(1).getIndex());
		//			if (!isOK1 && !isOK2) {
		//				DebugHolder.eliminated();
		//				return false;
		//			}
		//		}

		// Bottoms validation
		double bottomBandLimit = min + BOTTOM_BAND_RATIO * rangeHeight;
		List<Candle> bottomsInsideBand = sortedBottomsList.stream()
				.filter(c -> c.getLow() <= bottomBandLimit)
				.toList();
		bottomsInsideBand = filterExtremums(bottomsInsideBand, 5);

		//CHOCH validation
		boolean isCHOCH = isCHOCH(new ArrayList<>(maxList), new ArrayList<>(minList), currentCandle);
		if (isCHOCH) {
			DebugHolder.eliminated("Bearish CHOCH detected");
			return false;
		}

		boolean bottomsAreOk = bottomsInsideBand.size() >= NB_BOTTOMS_REQUIRED;
		if (!bottomsAreOk) {
			DebugHolder.eliminated("Bottoms  are not valid");
			return false;
		}
		return true;
	}

	boolean existsBetween(List<Candle> l1, List<Candle> l2) {
		int min = l1.stream().mapToInt(Candle::getIndex).min().getAsInt();
		int max = l1.stream().mapToInt(Candle::getIndex).max().getAsInt();

		for (int n : l2.stream().map(Candle::getIndex).toList()) {
			if (n > min && n < max) {
				return true;
			}
		}
		return false;
	}

	private boolean isBetweenIndexes(Integer index, Integer index1, Integer index2) {
		return (index > index1 && index < index2) || (index > index2 && index <index1);
	}

	public static List<Candle> filterExtremums(List<Candle> points, int minDistance) {
		List<Candle> result = new ArrayList<>();
		if (points.isEmpty()) return result;

		Candle last = points.get(0);
		result.add(last);

		for (int i = 1; i < points.size(); i++) {
			Candle current = points.get(i);
			if (Math.abs(current.getIndex() - last.getIndex()) >= minDistance) {
				result.add(current);
				last = current;
			}
		}
		return result;
	}

	private boolean isCHOCH(List<Candle> maxList, List<Candle> minList, Candle currentCandle) {
		minList.sort(Comparator.comparing(Candle::getIndex));
		maxList.sort(Comparator.comparing(Candle::getIndex));
		if (minList.size() < 2) {
			return false;
		}
		for (int i = 1; i < minList.size(); i++) {
			Candle min = minList.get(i);
			Candle minMinus1 = minList.get(i-1);
			Optional<Candle> maxBetween = maxList.stream().filter(m -> m.getIndex() > minMinus1.getIndex() && m.getIndex() < min.getIndex()).findFirst();
			if (maxBetween.isPresent()) {
				double height = maxBetween.get().getMax() - minMinus1.getMin();
				if (min.getMin() < minMinus1.getMin() + height*0.4 || currentCandle.getLow() < min.getMin()) {
					return false;
				}
			} else if (min.getMin() < minMinus1.getMin() || currentCandle.getLow() < min.getMin()) {
				return false;
			}
		}
		return true;
	}

	public Range getLargestRangeFast(List<Candle> candles, Candle currentCandle, List<Extremum> extremumsSwing) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		Double currentRangeHeight = null;
		int actualIndex = currentCandle.getIndex();
		double averageATR = getAverageATR(candles, actualIndex - 50, actualIndex);
		double maxRangeHeightLow = averageATR*MAX_RANGE_ATR_RATIO_LOW;
		double maxRangeHeightHigh = averageATR*MAX_RANGE_ATR_RATIO_HIGH;
		
		if (extremumsSwing.isEmpty()) {
			return null;
		}
		List<Extremum> extremumsReversed = extremumsSwing.stream()
				.filter(e -> e.getIndex() < actualIndex)
				.sorted(Comparator.comparingInt(Extremum::getIndex).reversed())
				.toList();

		Extremum lastExtremum = null;
		List<Candle> minList = new ArrayList<Candle>();
		List<Candle> maxList = new ArrayList<Candle>();
		DebugHolder.info();
		
		for (Extremum e :extremumsReversed) {
			DebugHolder.message("");
			DebugHolder.message("Looking at Extremum " + e.getDate());
			DebugHolder.info();
			Candle c = e.getCandle();
			//			if (lastExtremum != null 
			//					&& lastExtremum.isMin()
			//					&& isRangeValid(minList, maxList, candles.get(actualIndex))) {
			//				DebugHolder.info();
			//				return buildRange(candles, actualIndex, min, max, minList, maxList, extremumsReversed, c.getDate());
			//			}

			//			double medianATR = getMedianATR(candles, e.getIndex(), actualIndex);

			if (!maxList.isEmpty() && !minList.isEmpty()) {
				currentRangeHeight = getMaxOfTops(maxList).getMax() - getMinOfBottoms(minList).getMin();
			}

			if (e.isMax()) {
				//				if (c.isDate(25, 8) && c.isTime(13, 25, 0)) {
				//					getClass();
				//				}
				if (maxList.size() >= 5) {
					DebugHolder.message(c.getDate() + " enough tops and bottoms");
					return getRange(minList, maxList, candles.get(actualIndex), candles,
							min, max, extremumsReversed, c.getDate());
				} else	if (!minList.isEmpty()) {
					double potentialRangeHeight = c.getMax() - min;
					if (potentialRangeHeight <= maxRangeHeightLow) {
						DebugHolder.message(c.getDate() + " is valid max");
					} else if (potentialRangeHeight <= maxRangeHeightHigh) {
						if (maxList.isEmpty()) {
							DebugHolder.message(c.getDate() + " is valid max");
						} else {
							if (currentRangeHeight == null || potentialRangeHeight >= 1.5*currentRangeHeight ) {
								DebugHolder.message(c.getDate() + " new max out of bounds");
								return getRange(minList, maxList, candles.get(actualIndex), candles,
										min, max, extremumsReversed, c.getDate());
							} else {
								DebugHolder.message(c.getDate() + " is valid max");
							}
						}
					} else {
						DebugHolder.message(c.getDate() + " new max out of bounds");
						return getRange(minList, maxList, candles.get(actualIndex), candles,
								min, max, extremumsReversed, c.getDate());
					}
				}
				maxList.add(e.getCandle());
				if (c.getMax() > max) {
					max = c.getMax();
				}
			}
			if (e.isMin()) {
				if (!maxList.isEmpty()) {
					double potentialRangeHeight = max - c.getMin();
					if (potentialRangeHeight <= maxRangeHeightLow) {
						DebugHolder.message(c.getDate() + " is valid min");
					} else if (potentialRangeHeight <= maxRangeHeightHigh) {
						if (minList.isEmpty()) {
							DebugHolder.message(c.getDate() + " is valid min");
						} else {
							if (currentRangeHeight == null || potentialRangeHeight >= 1.5*currentRangeHeight ) {
								DebugHolder.eliminated("Min out of limits so range comes from bottom");
								return null;
							} else {
								DebugHolder.message(c.getDate() + " is valid min");
							}
						}
					} else {
						DebugHolder.eliminated("Min out of limits so range comes from bottom");
						return null;
					}
				}
				minList.add(e.getCandle());
				if (c.getMin() < min) {
					min = c.getMin();
				}
			}
			lastExtremum = e;
		}
		DebugHolder.eliminated("No range found");
		return null;
	}

	private boolean isConditionOnRangeValid(Range range, List<Extremum> extremumsSwing, List<Candle> candles) {
		DebugHolder.info();
		double median = range.getMin() + range.getHeight()*0.5d;
		Optional<Extremum> maxbeforeOpt = getFirstExtremumBefore(candles.get(range.getIndexStart()), extremumsSwing,
				ExtremumType.MAX, false);
		if (maxbeforeOpt.isEmpty()) {
			DebugHolder.eliminated("No max before range");
			return false;
		}
		int startIndexCountCandlesBefore = range.getIndexStart();
		for (int i = range.getIndexStart(); i < range.getIndexEnd(); i++) {
			if (candles.get(i).getLow() <= median ) {
				startIndexCountCandlesBefore = i;
				break;
			}
		}
		
		for (int i = startIndexCountCandlesBefore - NB_CANDLES_BEFORE; i < maxbeforeOpt.get().getIndex(); i++) {
			if (i < 0) {
				DebugHolder.eliminated("Not enought candles before range");
				return false;
			}
			if (candles.get(i).getLow() < median) {
				DebugHolder.eliminated("Candles before range are too low");
				return false;
			}
		}
		boolean rsiOK = false;
//		for (int i = maxbeforeOpt.get().getIndex();  i <= range.getSellingClimaxIndex(); i++) {
//			if (candles.get(i).getRsi() <= RSI_LIMIT) {
//				rsiOK = true;
//		        break;
//			}
//		}
//		if (!rsiOK) {
//			DebugHolder.eliminated("RSI NOT OK on first leg");
//			return false;
//		}
		
		DebugHolder.stopHere();
		int width = range.getIndexEnd() - range.getIndexStart();
		if (width < MIN_RANGE_WIDTH) {
			DebugHolder.eliminated("Range too narrow");
			return false;
		}
		if (width > MAX_RANGE_WIDTH) {
			DebugHolder.eliminated("Range too wide");
			return false;
		}
		if (!isRepartitionBalanced(range, candles)) {
			DebugHolder.eliminated("Candles repartition is not valid");
			return false;
		}
		return true;
	}

	private boolean isRepartitionBalanced(Range range, List<Candle> candles) {
		Integer startIndex = range.getIndexStart();
		Candle candleStart = candles.get(startIndex);
		Integer endIndex = range.getIndexEnd();
		int indexMiddle = (startIndex + endIndex) / 2;
		Double min = range.getMinWithLow();
		Double max = range.getMaxWithHigh();
		Double middlePrice = (min + max) / 2;
		Double height = max - min;
		double lowBand = middlePrice - height*0;
		double highBand = middlePrice + height*0;
		Map<Integer, AtomicInteger> map = Map.of(0, new AtomicInteger(0), 1, new AtomicInteger(0), 2,
				new AtomicInteger(0), 3, new AtomicInteger(0));
		for (int i = startIndex; i <= endIndex; i++) {
			Candle c = candles.get(i);
			if (i < indexMiddle && c.getHigh() >= highBand) {
				map.get(0).incrementAndGet();
			}
			if (i < indexMiddle && c.getLow() <= lowBand) {
				map.get(1).incrementAndGet();
			}
			if (i >= indexMiddle && c.getHigh() >= highBand) {
				map.get(2).incrementAndGet();
			}
			if (i >= indexMiddle && c.getLow() <= lowBand) {
				map.get(3).incrementAndGet();
			}
		}
		int total = endIndex - startIndex;
		//		int total = map.values().stream().mapToInt(v -> v.get()).sum();
		//		boolean isInvalid = map.values().stream().anyMatch(count -> count.get() * 1d / total * 1d < 0.15);
		boolean isInvalid = map.values().stream().anyMatch(count -> count.get() <= 5);
		//		if (candleStart.isDate(27, 4) && candleStart.isTime(8, 35, 0)) {
		//			getClass();
		//		}
		if (isInvalid) {
			DebugHolder.eliminated("Candle repartition ot valid");
			//			increaseCount("REPARTITION NOT VALID");
		}
		return !isInvalid;
	}
	private Range getRange(List<Candle> minList, List<Candle> maxList, Candle currentCandle, List<Candle> candles,
			double min, double max, List<Extremum> extremumsReversed, LocalDateTime dateDecisionRangeValid) {
//		minList.sort(Comparator.comparing(Candle::getIndex));
//		maxList.sort(Comparator.comparing(Candle::getIndex));
		while (isPrevalidated(minList, maxList, currentCandle)) {
			DebugHolder.info();
			Range range = buildRange(candles, currentCandle.getIndex(), min, max, minList, maxList, dateDecisionRangeValid);
			if (range != null) {
				if (isConditionOnRangeValid(range, extremumsReversed, candles)) {
					return range;
				} else {
					Candle maxRemoved = maxList.removeLast();
					minList = minList.stream().filter(c -> c.getIndex() > maxRemoved.getIndex()).toList();
					continue;
				}
			}
			return null;
		}
		DebugHolder.eliminated("Range is not valid");
		return null;
	}

	public Candle getLastExtremum(List<Candle> minList, List<Candle> maxList) {
		List<Candle> extremumList = new ArrayList<>(minList);
		extremumList.addAll(maxList);
		return extremumList.stream()
				.sorted(Comparator.comparingInt(Candle::getIndex).reversed()).findFirst().get();
	}

	public double getAverageATR(List<Candle> candles, Integer startIndex, int endIndex) {
		double average = 0;
		for (int i = Math.max(0, startIndex); i <= endIndex; i++) {
			average += candles.get(i).getAtr();
		}
		return average/(endIndex - startIndex + 1);
	}

	public Candle getFirstExtremum(List<Candle> minList, List<Candle> maxList) {
		List<Candle> extremumList = new ArrayList<>(minList);
		extremumList.addAll(maxList);
		return extremumList.stream()
				.sorted(Comparator.comparingInt(Candle::getIndex)).findFirst().get();
	}

	public Range buildRange(List<Candle> candles, int endIndex, double min, double max,
			List<Candle> minList, List<Candle> maxList, LocalDateTime dateDecisionRangeValid) {
		DebugHolder.info();
		Candle endCandle = candles.get(endIndex);
		double height = max - min;
		double bottomBand = min + height*0.2;
		Optional<Candle> firstMinNearBottom = minList.stream()
				.filter(c -> c.getMin() < bottomBand)
				.sorted(Comparator.comparingInt(Candle::getIndex))
				.findFirst();
		if (firstMinNearBottom.isEmpty()) {
			return null;
		}
		Integer startIndex = firstMinNearBottom.get().getIndex();
		max = getMaxWithHigh(candles, startIndex + 1, endIndex);

		//		Candle firstCandle = getFirstExtremum(minList, maxList);

		//		Integer firstIndex = firstCandle.getIndex();
		//		int startIndex = -1;
		for (int i = 1; i < startIndex; i++) {
			Candle cTmp = candles.get(startIndex - i);
			if (cTmp.getMin() < min) {
				DebugHolder.eliminated("Range comes from bottom");
				return null;
			}
			if (cTmp.getMax() > max + cTmp.getAtr()*0.25) {
				startIndex = cTmp.getIndex() + 1;
				break;
			}
		}
		try {
			
		double median = MedianCalculator.median(candles.subList(startIndex, endIndex));
		if (endCandle.getClose() >= median) {
			DebugHolder.eliminated("Last candle should be below median");
			return null;
		}
		} catch (Exception e) {
			getClass();
		}
		
		
		//		for (int i = startIndex; i < endIndex; i++) {
		//			if (candles.get(i).getMin() < median) {
		//				startIndex = i;
		//				break;
		//			}
		//		}
		//Rebuild min & max
		//		for (int i = startIndex; i < endIndex; i++) {
		//			if (candles.get(i).getMin() < median && candles.get(i).getMax() <= max) {
		//				startIndex = i;
		//				break;
		//			}
		//		}

		if (startIndex < 0) {
			return null;
		}
		if (endIndex < 0) {
			return null;
		}
		if (startIndex >= endIndex) {
			return null;
		}
		Candle lastExtremum = getLastExtremum(minList, maxList);
		min = getMinWithLow(candles, startIndex, lastExtremum.getIndex());
		max = getMaxWithHigh(candles, startIndex, lastExtremum.getIndex());
		
		DebugHolder.stopHere();
		int nbCandlesUnderMin =  countCandlesUnderMin(candles, min, startIndex, endIndex);
		if (nbCandlesUnderMin > 1) {
			// Why ? if price goes many times under min but is not a min yet
			DebugHolder.eliminated("Too many candles under min");
			return null;
		}
		return Range.builder()
				.indexEnd(endIndex)
				.dateStart(candles.get(startIndex).getDate())
				.dateEnd(candles.get(endIndex).getDate())
				.indexStart(startIndex)
				.indexRangeValidated(endIndex)
				.min(min)
				.sellingClimaxIndex(firstMinNearBottom.get().getIndex())
				.height(max - min)
				.minWithLow(getMinWithLow(candles, startIndex, endIndex))
				.maxWithHigh(getMaxWithHigh(candles, startIndex, endIndex))
				.width(endIndex - startIndex)
				.dateDecisionRangeValid(dateDecisionRangeValid)
				.minList(minList)
				.maxList(maxList)
				.max(max)
				.build();
	}

	private int countCandlesUnderMin(List<Candle> candles, double min, Integer startIndex, int endIndex) {
		int count = 0;
		for (int i = startIndex; i <= endIndex; i++) {
			if (candles.get(i).getMin() < min) {
				count++;
			}
		}
		return count;
	}

	private Double getMaxWithHigh(List<Candle> candles, int startIndex, int endIndex) {
		Double max = Double.MIN_VALUE;
		for (int i = startIndex; i < endIndex; i++) {
			max = Math.max(candles.get(i).getHigh(), max);
		}
		return max;
	}

	Double getMinWithLow(List<Candle> candles, int startIndex, int endIndex){
		Double min = Double.MAX_VALUE;
		for (int i = startIndex; i < endIndex; i++) {
			min = Math.min(candles.get(i).getLow(), min);
		}
		return min;
	}
}

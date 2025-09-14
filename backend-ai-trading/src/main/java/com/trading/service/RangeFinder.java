package com.trading.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.trading.dto.Range;
import com.trading.entity.Candle;
import com.trading.indicator.MedianCalculator;
import com.trading.indicator.extremum.Extremum;

@Component
public class RangeFinder extends AbstractService {

	private static final Double MAX_RANGE_ATR_RATIO = 4.5d;
	private static final Double MIN_RANGE_ATR_RATIO = 1d;
	private static final double BOTTOM_BAND_RATIO = 0.2d;
	private static final double TOP_BAND_RATIO = 0.35d;
	private static final double BREAK_DOWN_ATR_RATIO = 1;
	private static final double BREAK_UP_ATR_RATIO = 0.5;
	private static final double NB_BOTTOMS_REQUIRED = 2;

	private boolean isRangeValid(List<Candle> minList, List<Candle> maxList, Candle currentCandle) {
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
			return false;
		}
		
		// tops validation
		double topBandLimit = max - rangeHeight*TOP_BAND_RATIO;
		long nbTopsInsideBand = maxList.stream()
				.filter(c -> c.getMax() > topBandLimit)
				.count(); 
		boolean topsAreOk = nbTopsInsideBand >= NB_BOTTOMS_REQUIRED;
		
		// At least one very top between bottoms
		if (sortedBottomsList.size() >= 2 && sortedTopsList.size() >=2) {
			boolean isOK1 = isBetweenIndexes(sortedTopsList.get(0).getIndex(), sortedBottomsList.get(0).getIndex(), sortedBottomsList.get(1).getIndex());
			boolean isOK2 = isBetweenIndexes(sortedTopsList.get(1).getIndex(), sortedBottomsList.get(0).getIndex(), sortedBottomsList.get(1).getIndex());
			if (!isOK1 && !isOK2) {
				return false;
			}
		}
		
		// Bottoms validation
		double bottomBandLimit = min + BOTTOM_BAND_RATIO * rangeHeight;
		List<Candle> bottomsInsideBand = sortedBottomsList.stream()
				.filter(c -> c.getLow() <= bottomBandLimit)
				.toList();
		bottomsInsideBand = filterExtremums(bottomsInsideBand, 5);
		
		//CHOCH validation
		boolean isCHOCH = isCHOCH(maxList, minList, currentCandle);
		if (isCHOCH) {
			return false;
		}
		
		boolean bottomsAreOk = bottomsInsideBand.size() >= NB_BOTTOMS_REQUIRED;
		return  bottomsAreOk && topsAreOk;
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

	public Range getLargestRangeFast(List<Candle> candles, int actualIndex, List<Extremum> extremumsSwing, Double atr) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		if (extremumsSwing.isEmpty()) {
			return null;
		}
		List<Extremum> extremumsReversed = extremumsSwing.stream()
				.filter(e -> e.getIndex() < actualIndex)
				.sorted(Comparator.comparingInt(Extremum::getIndex).reversed())
				.toList();

		Extremum lastExtremum = extremumsReversed.get(0);
		List<Candle> minList = new ArrayList<Candle>();
		List<Candle> maxList = new ArrayList<Candle>();
		Candle tmp = candles.get(actualIndex);
		
//		if (tmp.isDate(27, 5) && tmp.isTime(9, 5, 0)) {
//			debug = true;
//		} else {
//			debug = false;
//		}
		for (Extremum e :extremumsReversed) {
			DebugHolder.message("Looking at Extremum " + e.getDate());
			DebugHolder.info();
			Candle c = e.getCandle();
			if (isRangeValid(minList, maxList, candles.get(actualIndex))) {
				DebugHolder.info();
				return buildRange(candles, actualIndex, min, max, minList, maxList, extremumsReversed, c.getDate());
			}

			//			double medianATR = getMedianATR(candles, e.getIndex(), actualIndex);

			double maxRangeHeight = atr*MAX_RANGE_ATR_RATIO;

			if (e.isMax()) {
				//				if (c.isDate(25, 8) && c.isTime(13, 25, 0)) {
				//					getClass();
				//				}
				if (!minList.isEmpty()) {
					double potentialRangeSize = c.getMax() - min;
					if (potentialRangeSize > maxRangeHeight) {
						boolean rescueTop = false;
						if (!maxList.isEmpty()) {
							Candle maxTop = getMaxOfTops(maxList);
							if (c.getMax() <= maxTop.getMax() ||
									Math.abs(maxTop.getMax() - c.getMax()) < BREAK_UP_ATR_RATIO *c.getAtr()) {
								rescueTop = true;
								c.setRescued(true);
							}
						}
						if (!rescueTop) {
							DebugHolder.message(c.getDate() + " new max out of bounds");
							DebugHolder.eliminated();
							return null;
						}
					} else {
						DebugHolder.message(c.getDate() + " is valid max");
					}
				}
				maxList.add(e.getCandle());
				if (c.getMax() > max) {
					max = c.getMax();
				}
			}
			if (e.isMin()) {
				if (!maxList.isEmpty()) {
					double potentialRangeSize = max - c.getMin();
					if (potentialRangeSize > maxRangeHeight) {
						boolean rescueBottom = false;
						if (!minList.isEmpty()) {
							Candle minBottom = getMinOfBottoms(minList);
							if (c.getMin() >= minBottom.getMin() || 
									Math.abs(minBottom.getMin() - c.getMin()) < BREAK_DOWN_ATR_RATIO *c.getAtr()) {
								rescueBottom = true;
								c.setRescued(true);
							}
						}
						if (!rescueBottom) {
							DebugHolder.eliminated();
							return null;
						}
					} else {
						DebugHolder.message(c.getDate() + " is valid min");
					}
				}
				minList.add(e.getCandle());
				if (c.getMin() < min) {
					min = c.getMin();
				}
			}
		}
		DebugHolder.eliminated();
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
		for (int i = startIndex; i <= endIndex; i++) {
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

	public Range buildRange(List<Candle> candles, int actualIndex, double min, double max,
			List<Candle> minList, List<Candle> maxList, List<Extremum> extremumsReversed, LocalDateTime dateDecisionRangeValid) {
		Candle firstCandle = getFirstExtremum(minList, maxList);

		Integer firstIndex = firstCandle.getIndex();
		Boolean breakFromTop = null;
		int startIndex = -1;
		for (int i = 1; i < firstIndex; i++) {
			Candle cTmp = candles.get(firstIndex - i);
			if (cTmp.getMin() < min) {
				return null;
			}
			if (cTmp.getMax() > max + BREAK_UP_ATR_RATIO * cTmp.getAtr()) {
				startIndex = firstIndex -i +1;
				break;
			}
		}
		int endIndex = actualIndex;
		double median = MedianCalculator.median(candles.subList(startIndex, endIndex));
		for (int i = startIndex; i < endIndex; i++) {
			if (candles.get(i).getMin() < median) {
				startIndex = i;
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
				.indexRangeValidated(actualIndex)
				.breakFromTop(breakFromTop)
				.min(min)
				.minWithLow(getMinWithLow(candles, startIndex, endIndex))
				.maxWithHigh(getMaxWithHigh(candles, startIndex, endIndex))
				.width(endIndex - startIndex)
				.dateDecisionRangeValid(dateDecisionRangeValid)
				.minList(minList)
				.maxList(maxList)
				.max(max)
				.build();
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

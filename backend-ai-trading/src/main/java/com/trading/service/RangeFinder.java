package com.trading.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
		double topBandLimit = max - rangeHeight*TOP_BAND_RATIO;
		long nbTopsInsideBand = maxList.stream()
				.filter(c -> c.getMax() > topBandLimit)
				.count(); 
		boolean topsAreOk = nbTopsInsideBand >= NB_BOTTOMS_REQUIRED;
		double bottomBandLimit = min + BOTTOM_BAND_RATIO * rangeHeight;
		List<Candle> bottomsInsideBand = sortedBottomsList.stream()
				.filter(c -> c.getLow() <= bottomBandLimit)
				.toList();
		bottomsInsideBand = filterExtremums(bottomsInsideBand, 5);
		boolean isCHOCH = isCHOCH(minList, currentCandle);
		boolean bottomsAreOk = bottomsInsideBand.size() >= NB_BOTTOMS_REQUIRED;
		if (isCHOCH) {
			return false;
		}
		return  bottomsAreOk && topsAreOk;
	}

	public static List<Candle> filterExtremums(List<Candle> points, int minDistance) {
		List<Candle> result = new ArrayList<>();
		if (points.isEmpty()) return result;

		// On prend le premier point
		Candle last = points.get(0);
		result.add(last);

		// On parcourt la liste
		for (int i = 1; i < points.size(); i++) {
			Candle current = points.get(i);
			if (current.getIndex() - last.getIndex() >= minDistance) {
				result.add(current);
				last = current;
			}
		}
		return result;
	}

	private boolean isCHOCH(List<Candle> minList, Candle currentCandle) {
		minList.sort(Comparator.comparing(Candle::getIndex));
		if (minList.size() < 2) {
			return false;
		}
		for (int i = 1; i < minList.size(); i++) {
			if (minList.get(i).getMin() < minList.get(i-1).getMin() || currentCandle.getLow() < minList.get(i).getMin()) {
				return false;
			}
		}
		return true;
	}
	
	public Range getLargestRangeFast(List<Candle> candles, int actualIndex, List<Extremum> extremumsSwing) {
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
		//		if (tmp.isDate(14, 7) && tmp.isTime(7, 40, 0)) {
		//			debug = true;
		//		}
		for (Extremum e :extremumsReversed) {
			debug("Looking at Extremum " + e.getDate());
			if (debug) {
				getClass();
			}
			Candle c = e.getCandle();
			if (isRangeValid(minList, maxList, candles.get(actualIndex))) {
				debug(c.getDate() + " return valid range");
				return buildRange(candles, actualIndex, min, max, minList, maxList, extremumsReversed, c.getDate());
			}

			Double averageATR = null;
			if (e.getIndex() == lastExtremum.getIndex()) {
				averageATR = e.getCandle().getAtr();
			} else {
				averageATR = getAverageATR(candles, e.getIndex(), lastExtremum.getIndex());
			}
			//			double medianATR = getMedianATR(candles, e.getIndex(), actualIndex);

			double maxRangeHeight = averageATR*MAX_RANGE_ATR_RATIO;

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
							debug(c.getDate() + " new max out of bounds");
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

	private Candle getLastExtremum(List<Candle> minList, List<Candle> maxList) {
		List<Candle> extremumList = new ArrayList<>(minList);
		extremumList.addAll(maxList);
		return extremumList.stream()
				.sorted(Comparator.comparingInt(Candle::getIndex).reversed()).findFirst().get();
	}

	private double getAverageATR(List<Candle> candles, Integer startIndex, int endIndex) {
		double average = 0;
		for (int i = startIndex; i <= endIndex; i++) {
			average += candles.get(i).getAtr();
		}
		return average/(endIndex - startIndex + 1);
	}

	private Candle getFirstExtremum(List<Candle> minList, List<Candle> maxList) {
		List<Candle> extremumList = new ArrayList<>(minList);
		extremumList.addAll(maxList);
		return extremumList.stream()
				.sorted(Comparator.comparingInt(Candle::getIndex)).findFirst().get();
	}
	
	private Range buildRange(List<Candle> candles, int actualIndex, double min, double max,
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
				.width(endIndex - startIndex)
				.dateDecisionRangeValid(dateDecisionRangeValid)
				.minList(minList)
				.maxList(maxList)
				.max(max)
				.build();
	}
}

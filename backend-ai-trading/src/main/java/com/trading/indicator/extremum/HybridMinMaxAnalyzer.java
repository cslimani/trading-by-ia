package com.trading.indicator.extremum;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import com.trading.entity.Candle;
import com.trading.enums.ExtremumType;

import lombok.Getter;

public class HybridMinMaxAnalyzer {
	
	private Candle potentialMax;
	private Candle potentialMin;
	private Candle potentialMaxAfterMin;
	private Candle potentialMinAfterMax;
	private Candle lastMax;
	private Candle lastMin;
	private List<Candle> minList;
	private List<Candle> maxList;
	@Getter
	private List<Extremum> extremumList = new ArrayList<Extremum>();
	private double ratioATR;
	private int k;
	private double atr;
	CircularFifoQueue<Double> atrQueue = new CircularFifoQueue<Double>(5);
	
	public HybridMinMaxAnalyzer(double thresoldAtr, int k) {
		this.ratioATR = thresoldAtr;
		minList = new ArrayList<Candle>();
		maxList = new ArrayList<Candle>();
		this.k = k;
	}
	
	public List<Extremum> process(Candle candle) {
//		System.out.println(candle.getDate());
		atrQueue.add(candle.getAtr());
		atr = atrQueue.stream().mapToDouble(a -> a).average().getAsDouble();
//		if (candle.isDate(2, 1) && candle.isTime(11, 50, 0)) {
//			getClass();
//		}
		if (potentialMaxAfterMin == null || candle.getMax() > potentialMaxAfterMin.getMax()) {
			potentialMaxAfterMin = candle;
//			if (potentialMaxAfterMin.isDate(7, 1) && potentialMaxAfterMin.isTime(1, 45, 0)) {
//				getClass();
//			}
		}
		if (potentialMinAfterMax == null || candle.getMin() < potentialMinAfterMax.getMin()) {
			potentialMinAfterMax = candle;
		}
		if (lastMin != null && lastMax != null) {
			if (lastMin.isAfter(lastMax)  && (potentialMax == null || candle.getMax() > potentialMax.getMax())) {
				setPotentialMax(candle, null);
			}
			if (lastMax.isAfter(lastMin)  && (potentialMin == null || candle.getMin() < potentialMin.getMin())) {
				setPotentialMin(candle, null);
			}
			if (potentialMax != null &&
					potentialMax.getDate().isBefore(candle.getDate())
					&& isConditionForMax(candle)
					&& lastMax.getDate().isBefore(lastMin.getDate())) {
				addMax(candle);
			}
			if (potentialMin != null && potentialMin.getDate().isBefore(candle.getDate())
					&& isConditionForMin(candle)
					&& lastMin.getDate().isBefore(lastMax.getDate())) {
				addMin(candle);
			}
		} else {
			if (lastMax == null && (potentialMax == null || candle.getMax() > potentialMax.getMax())) {
				setPotentialMax(candle, null);
			}
			if (lastMin == null && (potentialMin == null ||candle.getMin() < potentialMin.getMin())) {
				setPotentialMin(candle, null);
			}
			if (lastMin == null 
					&& potentialMin.getDate().isBefore(candle.getDate())
					&& isConditionForMin(candle)) {
				addMin(candle);
			}
			if ( lastMax == null
					&& potentialMax.getDate().isBefore(candle.getDate())
					&& isConditionForMax(candle)) {
				addMax(candle);
			}
		}
		return extremumList;
	}

	private void setPotentialMax(Candle newPotentialMax, Candle currentCandle) {
		potentialMax = newPotentialMax;
		potentialMinAfterMax = currentCandle;
	}
	
	private void setPotentialMin(Candle newPotentialMin, Candle currentCandle) {
		potentialMin = newPotentialMin;
//		if (currentCandle != null && currentCandle.isDate(7, 1) && currentCandle.isTime(1, 45, 0)) {
//			getClass();
//		}
		potentialMaxAfterMin = currentCandle;
	}

	private boolean isConditionForMin(Candle candle) {
		return potentialMin != null && ((candle.getMax()  > potentialMin.getMin()  + ratioATR * atr) ||
				(candle.getIndex() - potentialMin.getIndex() > k));
	}

	private boolean isConditionForMax(Candle candle) {
		return potentialMax != null && ((candle.getMin() < potentialMax.getMax()  - ratioATR * atr) ||
				(candle.getIndex() - potentialMax.getIndex() > k));
	}

	private void addMin(Candle candle) {
		Candle newMin = potentialMin;
//		if (newMin.isDate(7, 1) && newMin.isTime(1, 45, 0)) {
//			getClass();
//		}
		potentialMin = null;
		if (lastMax != null && newMin.isBefore(lastMax)) {
			throw new RuntimeException("new min is before lastMax");
		}
		minList.add(newMin);
		extremumList.add(new Extremum(newMin, newMin.getDate(), newMin.getMin(), ExtremumType.MIN, null));
		setPotentialMax(potentialMaxAfterMin, null);
		lastMin = newMin;
//		System.out.println("New min at " + newMin.getDate());
	}

	private void addMax(Candle candle) {
		Candle newMax = potentialMax;
//		if (newMax.isDate(7, 1) && newMax.isTime(1, 45, 0)) {
//			getClass();
//		}
		if (lastMax != lastMin &&  newMax.isBefore(lastMin)) {
			throw new RuntimeException("new max is before lastMin");
		}
		potentialMax = null;
		maxList.add(newMax);
		extremumList.add(new Extremum(newMax, newMax.getDate(), newMax.getMax(), ExtremumType.MAX, null));
		setPotentialMin(potentialMinAfterMax, null);
		lastMax = newMax;
//		System.out.println("New max at " + newMax.getDate());
	}


}

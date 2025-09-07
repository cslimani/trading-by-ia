package com.trading.indicator.extremum;

import java.util.ArrayList;
import java.util.List;

import com.trading.entity.Candle;
import com.trading.enums.ExtremumType;

import lombok.Getter;

public class MinMaxAnalyzer {
	
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
	
	public MinMaxAnalyzer(double thresoldAtr) {
		this.ratioATR = thresoldAtr;
		minList = new ArrayList<Candle>();
		maxList = new ArrayList<Candle>();
	}
	
	public List<Extremum> process(Candle candle) {
//		System.out.println(candle.getDate() + " " + candle.getMax());
		if (lastMin != null && lastMax != null) {
			if ( lastMin.isAfter(lastMax)  && (potentialMax == null || candle.getMax() > potentialMax.getMax())) {
				potentialMax = candle;
			}
			if (lastMax.isAfter(lastMin)  && (potentialMin == null || candle.getMin() < potentialMin.getMin())) {
				potentialMin = candle;
			}
			if (potentialMaxAfterMin == null || candle.getMax() > potentialMaxAfterMin.getMax()) {
				potentialMaxAfterMin = candle;
			}
			if (potentialMinAfterMax == null || candle.getMin() < potentialMinAfterMax.getMin()) {
				potentialMinAfterMax = candle;
			}
			if (potentialMax != null &&
					potentialMax.getDate().isBefore(candle.getDate())
					&& candle.getMin() < potentialMax.getMax()  - ratioATR * candle.getAtr()
//					&& lastMin.getMin() < potentialMax.getMax()  - ratioATR * candle.getAtr() 
					&& lastMax.getDate().isBefore(lastMin.getDate())) {
				addMax(candle);
			}
			if (potentialMin != null && potentialMin.getDate().isBefore(candle.getDate())
					&& candle.getMax()  > potentialMin.getMin()  + ratioATR * candle.getAtr()
//					&& lastMax.getMax() > potentialMin.getMin()  + ratioATR * candle.getAtr()
					&& lastMin.getDate().isBefore(lastMax.getDate())) {
				addMin(candle);
			}
		} else {
			if (lastMax == null && (potentialMax == null || candle.getMax() > potentialMax.getMax())) {
				potentialMax = candle;
			}
			if (lastMin == null && (potentialMin == null ||candle.getMin() < potentialMin.getMin())) {
				potentialMin = candle;
			}
			if (lastMin == null 
					&& potentialMin.getDate().isBefore(candle.getDate())
					&& candle.getMax()  > potentialMin.getMin()  + ratioATR * candle.getAtr()) {
				addMin(candle);
			}
			if ( lastMax == null
					&& potentialMax.getDate().isBefore(candle.getDate())
					&& candle.getMin() < potentialMax.getMax()  - ratioATR * candle.getAtr() ) {
				addMax(candle);
			}
		}
		return extremumList;
	}

	private void addMin(Candle candle) {
		minList.add(potentialMin);
		extremumList.add(new Extremum(potentialMin, potentialMin.getDate(), potentialMin.getMin(), ExtremumType.MIN, null));
		potentialMax = candle;
		potentialMaxAfterMin = candle;
		lastMin = potentialMin;
		System.out.println("New min at " + potentialMin.getDate());
		potentialMin = null;
	}

	private void addMax(Candle candle) {
		maxList.add(potentialMax);
		extremumList.add(new Extremum(potentialMax, potentialMax.getDate(), potentialMax.getMax(), ExtremumType.MAX, null));
		potentialMin = candle;
		potentialMinAfterMax = candle;
		lastMax = potentialMax;
		System.out.println("New max at " + potentialMax.getDate());
		potentialMax = null;
	}


}

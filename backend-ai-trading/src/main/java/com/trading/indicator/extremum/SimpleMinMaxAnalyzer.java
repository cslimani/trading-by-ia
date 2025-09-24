package com.trading.indicator.extremum;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import com.trading.entity.Candle;
import com.trading.enums.ExtremumType;

import lombok.Getter;

public class SimpleMinMaxAnalyzer {

	private List<Candle> potentialMinList;
	private List<Candle>  potentialMaxList;
	Candle potentialMin;
	Candle potentialMax;

	@Getter
	private List<Extremum> extremumList = new ArrayList<Extremum>();
	private double ratioATR;
	private int k;
	private double atr;
	CircularFifoQueue<Double> atrQueue = new CircularFifoQueue<Double>(5);
	private List<Candle> candles;
	private boolean debug = false;

	private void debug(String message){
		if (debug) {
			System.out.println(message);
			getClass();
		}
	}

	public SimpleMinMaxAnalyzer(double thresoldAtr, int k) {
		this.ratioATR = thresoldAtr;
		potentialMinList = new ArrayList<Candle>();
		potentialMaxList = new ArrayList<Candle>();
		this.k = k;
		this.candles = new ArrayList<Candle>();
	}

	public List<Extremum> process(Candle c) {
		debug("Process " + c.getDate());
		atrQueue.add(c.getAtr());
		atr = atrQueue.stream().mapToDouble(a -> a).average().getAsDouble();
		candles.add(c);

//		if (c.isDate(2, 1) && c.isTime(4, 45, 0)) {
//			getClass();
//		}

		processPotentialMax(c);
		processPotentialMin(c);

		if (isMaxBeforeCount(c) || isMaxBeforeAtr(c)) {
			debug("Add potential max " + c.getDate());
			addPotentialMax(c);
		}
		if (isMinBeforeCount(c) || isMinBeforeAtr(c)) {
			debug("Add potential min " + c.getDate());
			addPotentialMin(c);
		}

		return extremumList;
	}

	private void addPotentialMin(Candle c) {
		if (potentialMin == null || c.getMin() < potentialMin.getMin()) {
			potentialMin = c;
		}
		//		potentialMinList.add(c);
	}

	private void addPotentialMax(Candle c) {
		//		potentialMaxList.add(c);
		if (potentialMax == null || c.getMax() > potentialMax.getMax()) {
			potentialMax = c;
		}
	}

	//	private void processPotentialMax(Candle c) {
	//		for (Iterator<Candle> it = potentialMaxList.iterator(); it.hasNext();) {
	//			Candle potentialMax = it.next();
	//			if (c.getMax() > potentialMax.getMax()) {
	//				debug("Remove potential max " + potentialMax.getDate());
	//				it.remove();
	//				return;
	//			}
	//
	//			if (isMaxAfterCount(potentialMax, c) || isMaxAtr(potentialMax, c)) {
	//				extremumList.add(new Extremum(potentialMax, potentialMax.getDate(), potentialMax.getMax(), ExtremumType.MAX, null));
	//				it.remove();
	//				System.out.println(c.getDate() + "  : new max at "  + potentialMax.getDate());
	//			}
	//		}
	//	}
	//
	//	private void processPotentialMin(Candle c) {
	//		for (Iterator<Candle> it = potentialMinList.iterator(); it.hasNext();) {
	//			Candle potentialMin = it.next();
	//			if (c.getMin() < potentialMin.getMin()) {
	//				it.remove();
	//				debug("Remove potential min " + potentialMin.getDate());
	//				return;
	//			}
	//			if (isMinAfterCount(potentialMin, c) || isMinAtr(potentialMin, c)) {
	//				extremumList.add(new Extremum(potentialMin, potentialMin.getDate(), potentialMin.getMin(), ExtremumType.MIN, null));
	//				it.remove();
	//				System.out.println(c.getDate() + "  : new min at "  + potentialMin.getDate());
	//			}
	//		}
	//	}

	private void processPotentialMax(Candle c) {
		if (potentialMax == null) {
			return;
		}
		if (c.getMax() > potentialMax.getMax()) {
			debug("Remove potential max " + potentialMax.getDate());
			potentialMax = null;
			return;
		}

		if (isMaxAfterCount(potentialMax, c) || isMaxAtr(potentialMax, c)) {
			extremumList.add(new Extremum(potentialMax, potentialMax.getDate(), potentialMax.getMax(), ExtremumType.MAX, null));
//			System.out.println(c.getDate() + "  : new max at "  + potentialMax.getDate());
			potentialMax = null;
		}
	}

	private void processPotentialMin(Candle c) {
		if (potentialMin == null) {
			return;
		}
		if (c.getMin() < potentialMin.getMin()) {
			debug("Remove potential min " + potentialMin.getDate());
			potentialMin = null;
			return;
		}
		if (isMinAfterCount(potentialMin, c) || isMinAtr(potentialMin, c)) {
			extremumList.add(new Extremum(potentialMin, potentialMin.getDate(), potentialMin.getMin(), ExtremumType.MIN, null));
//			System.out.println(c.getDate() + "  : new min at "  + potentialMin.getDate());
			potentialMin = null;
		}
	}

	private boolean isMinBeforeCount(Candle c) {
		Integer index = c.getIndex();
		if (index < k) {
			return false;
		}
		for (int i = 1; i <= k; i++) {
			if (candles.get(index - i).getMin() < c.getMin()) {
				return false;
			}
		}
		return true;
	}

	private boolean isMinAfterCount(Candle potentialMin, Candle c) {
		if (c.getIndex() < potentialMin.getIndex() + k) {
			return false;
		}
		if (potentialMin.getIndex() + k >= candles.size()) {
			return false;
		}
		for (int i = 1; i <= k; i++) {
			if (candles.get(potentialMin.getIndex() + i).getMin() < potentialMin.getMin()) {
				return false;
			}
		}
		return true;
	}

	private boolean isMinBeforeAtr(Candle potentialMin) {
		Integer index = potentialMin.getIndex();
		for (int i = 1; i <= index; i++) {
			Candle c = candles.get(index - i);
			if (c.getMin() < potentialMin.getMin()) {
				return false;
			}
			if (isMinAtr(potentialMin, c)) {
				return true;
			}
		}
		return false;
	}

	private boolean isMinAtr(Candle potentialMin, Candle c) {
		return (c.getMax() - potentialMin.getMin()) > ratioATR * atr;
	}


	private boolean isMaxBeforeCount(Candle c) {
		Integer index = c.getIndex();
		if (index < k) {
			return false;
		}
		for(int i = 1; i <= k ; i++) {
			if (candles.get(index - i).getMax() > c.getMax()) {
				return false;
			}
		}
		return true;
	}

	private boolean isMaxAfterCount(Candle potentialMax,Candle c) {
		if (c.getIndex() < potentialMax.getIndex() + k) {
			return false;
		}
		if (potentialMax.getIndex() + k  >= candles.size()) {
			return false;
		}
		for(int i = 1; i <= k ; i++) {
			if (candles.get(potentialMax.getIndex() + i).getMax() > potentialMax.getMax()) {
				return false;
			}
		}
		return true;
	}

	private boolean isMaxBeforeAtr(Candle potentialMax) {
		Integer index = potentialMax.getIndex();
		for(int i = 1; i <= index; i++) {
			Candle c = candles.get(index-i);
			if (c.getMax() > potentialMax.getMax()) {
				return false;
			}
			if (isMaxAtr(potentialMax, c)) {
				return true;
			}
		}
		return false;
	}

	private boolean isMaxAtr(Candle potentialMax, Candle c) {
		return (potentialMax.getMax() - c.getMin()) > ratioATR * atr;
	}
}

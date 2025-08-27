package com.trading.indicator.extremum;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.trading.entity.Candle;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class CandleMinMaxAnalyzerMine {

	private ExtremumCandidate candidateMax;
	private ExtremumCandidate candidateMin;
	private Candle lastMax;
	private Candle lastMin;
	private List<Candle> minList = new ArrayList<Candle>();
	private List<Candle> maxList = new ArrayList<Candle>();
	private int nbCandels;
	private List<Candle> candles;


	public CandleMinMaxAnalyzerMine(int nbCandels, List<Candle> candles) {
		this.nbCandels = nbCandels;
		this.candles = candles;
	}

	public void process() {
		for (int i = 0; i < candles.size(); i++) {
			Candle c = candles.get(i);
			c.setIndex(i);
			if (lastMin != null && lastMax != null) {
				boolean followingMin = candidateMin.processMin(c);
				boolean followingMax = candidateMax.processMax(c);

				if (lastMax.isBefore(lastMin)) {
//					log.info(c.getDate() +  " - Look for Max");
//					if (!followingMax) {
//						candidateMin.reset(c);
//						log.info(c.getDate() +  " - Reset candidateMin");
//					}
					if (candidateMax.isValid()) {
						addMax(c);
					}
				}
				if (lastMin.isBefore(lastMax)) {
//					log.info(c.getDate() +  " - Look for Min");
//					if (!followingMin) {
//						candidateMax.reset(c);
//						log.info(c.getDate() +  " - Reset candidateMax");
//					}
					if (candidateMin.isValid()) {
						addMin(c);
					}
				}
			} else {
				if (candidateMin != null) {
					candidateMin.processMin(c);
				}
				if (candidateMax != null ) {
					candidateMax.processMax(c);
				}
				if (lastMax == null && (candidateMax == null || getMax(c) > candidateMax.getMax())) {
					candidateMax = new ExtremumCandidate(c, nbCandels);
				}
				if (lastMin == null && (candidateMin == null || getMin(c) < candidateMin.getMin())) {
					candidateMin = new ExtremumCandidate(c, nbCandels);
				}
				if (lastMax == null && candidateMax.isValid()) {
					addMax(c);
					candidateMin.reset(c);
				}
				if (lastMin == null && candidateMin.isValid()) {
					addMin(c);
					candidateMax.reset(c);
				}
			}
		}
	}

	private double getMax(Candle candle) {
		return candle.getMax();
	}

	private double getMin(Candle candle) {
		return candle.getMin();
	}

	private void addMin(Candle candle) {
		Candle potentialMin = candidateMin.getCandle();
		if (lastMin != null) {
			candidateMax.reset(lastMin);
		}
		for (int i = potentialMin.getIndex()+1; i <= candle.getIndex(); i++) {
			candidateMax.processMax(candles.get(i));
		}
		if (candidateMax.getCandle().getIndex() - potentialMin.getIndex() > nbCandels ) {
			log.info(candle.getDate() +  " - New min at " + candidateMin.getCandle().getDate());
			lastMin = potentialMin;
			minList.add(candidateMin.getCandle());
		}
	}

	private void addMax(Candle candle) {
		Candle potentialMax = candidateMax.getCandle();
		if (lastMax != null) {
			candidateMin.reset(lastMax);
		}
		for (int i = potentialMax.getIndex()+1; i <= candle.getIndex(); i++) {
			candidateMin.processMin(candles.get(i));
		}
		if (candidateMin.getCandle().getIndex() - potentialMax.getIndex() > nbCandels ) {
			log.info(candle.getDate() +  " - New max at " + candidateMax.getCandle().getDate());
			lastMax = potentialMax;
			maxList.add(candidateMax.getCandle());
		}
	}


	public Candle getLastMaxBeforeMin() {
		if (lastMin == null) {
			return lastMax;
		}
		for (Candle max : maxList) {
			if (max.isBefore(lastMin)) {
				return max;
			}
		}
		return null;
	}

	public Candle getLastMinBeforeMax() {
		if (lastMax == null) {
			return lastMin;
		}
		for (Candle min : minList) {
			if (min.isBefore(lastMax)) {
				return min;
			}
		}
		return null;
	}


}

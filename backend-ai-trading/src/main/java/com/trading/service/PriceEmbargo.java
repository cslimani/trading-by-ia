package com.trading.service;

import java.util.ArrayList;
import java.util.List;

import com.trading.entity.Candle;
import com.trading.indicator.AtrCalculator;

public class PriceEmbargo {

	List<Candle> forwardCandles;
	List<Candle> pastCandles = new ArrayList<>();
	AtrCalculator atrCalculator = new AtrCalculator(20);
	
	public PriceEmbargo(List<Candle> forwardCandles, boolean isSetIndex) {
		this(List.of(),forwardCandles, isSetIndex);
	}
	
	public PriceEmbargo(List<Candle> pastCandles, List<Candle> forwardCandles, boolean isSetIndex) {
		this.forwardCandles = new ArrayList<>(forwardCandles);
		this.pastCandles = new ArrayList<>(pastCandles);
		for (int i = 0; i < pastCandles.size(); i++) {
			atrCalculator.compute(pastCandles.get(i));
		}
		if (isSetIndex) {
			setIndex(forwardCandles);
		}
	}
	
	public Candle current() {
		return pastCandles.getLast();
	}
	
	
	public Candle goForward() {
		Candle c = forwardCandles.removeFirst();
		atrCalculator.compute(c);
		pastCandles.add(c);
		return c;
	}
	
	public boolean hasNext() {
		return 	!forwardCandles.isEmpty();
	}
	
	public int size() {
		return pastCandles.size();
	}
	
	public void setIndex(List<Candle> candles) {
		for (int i = 0; i < candles.size(); i++) {
			candles.get(i).setIndex(i);
		}
	}
	
	public PriceEmbargo clone() {
		return new PriceEmbargo(pastCandles, forwardCandles,false);
	}

	public Candle get(int i) {
		return pastCandles.get(i);
	}
	
	public List<Candle> getPastCandles() {
		return pastCandles;
	}
	
}

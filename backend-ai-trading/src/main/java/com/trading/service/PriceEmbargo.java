package com.trading.service;

import java.util.ArrayList;
import java.util.List;

import com.trading.entity.Candle;
import com.trading.indicator.AtrCalculator;

import lombok.NonNull;

public class PriceEmbargo {

	@NonNull
	List<Candle> allCandles;
	List<Candle> availableCandles = new ArrayList<>();
	AtrCalculator atrCalculator = new AtrCalculator(20);
	
	public PriceEmbargo(List<Candle> allCandles, List<Candle> availableCandles, boolean isSetIndex) {
		this.allCandles = new ArrayList<>(allCandles);
		this.availableCandles = new ArrayList<>(availableCandles);
		for (int i = 0; i < availableCandles.size(); i++) {
			atrCalculator.compute(availableCandles.get(i));
		}
		if (isSetIndex) {
			setIndex(allCandles);
		}
	}
	
	public Candle current() {
		return availableCandles.getLast();
	}
	
	public Candle goForward() {
		Candle c = allCandles.removeFirst();
		atrCalculator.compute(c);
		availableCandles.add(c);
		return c;
	}
	
	public boolean hasNext() {
		return 	!allCandles.isEmpty();
	}
	
	public int size() {
		return availableCandles.size();
	}
	
	public void setIndex(List<Candle> candles) {
		for (int i = 0; i < candles.size(); i++) {
			candles.get(i).setIndex(i);
		}
	}
	
	public PriceEmbargo clone() {
		return new PriceEmbargo(allCandles, availableCandles, false);
	}
	
}

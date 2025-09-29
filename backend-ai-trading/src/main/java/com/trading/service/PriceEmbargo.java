package com.trading.service;

import java.util.ArrayList;
import java.util.List;

import com.trading.entity.Candle;

import lombok.NonNull;

public class PriceEmbargo {

	@NonNull
	List<Candle> allCandles;
	List<Candle> availableCandles = new ArrayList<>();

	public PriceEmbargo(List<Candle> allCandles) {
		this.allCandles = allCandles;
		setIndex(allCandles);
	}
	
	public void goForward() {
		availableCandles.add(allCandles.removeFirst());		
	}
	
	public int size() {
		return availableCandles.size();
	}
	
	public void setIndex(List<Candle> candles) {
		for (int i = 0; i < candles.size(); i++) {
			candles.get(i).setIndex(i);
		}
	}
	
}

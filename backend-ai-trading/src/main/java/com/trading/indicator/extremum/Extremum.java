package com.trading.indicator.extremum;

import java.time.LocalDateTime;

import com.trading.entity.Candle;
import com.trading.indicator.extremum.SwingExtremaFinder.Type;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Extremum {

	public Candle candle;
	@Include
	public LocalDateTime date;
	public double price;
	public Type type;
	public Candle bosBreakingCandle;
	public Integer k;
	
	public Extremum(Candle candle, LocalDateTime date, double price, Type type, Integer k) {
		this.candle = candle;
		this.date = date;
		this.price = price;
		this.type = type;
		this.k = k;
	}
	
	public boolean isMax() {
		return type == Type.MAX;
	}

	public boolean isMin() {
		return type == Type.MIN;
	}
	
}

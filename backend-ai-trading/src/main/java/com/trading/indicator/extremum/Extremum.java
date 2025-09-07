package com.trading.indicator.extremum;

import java.time.LocalDateTime;

import com.trading.entity.Candle;
import com.trading.enums.ExtremumType;

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
	public ExtremumType type;
	public Candle bosBreakingCandle;
	public Integer k;
	
	public Extremum(Candle candle, LocalDateTime date, double price, ExtremumType type, Integer k) {
		this.candle = candle;
		this.date = date;
		this.price = price;
		this.type = type;
		this.k = k;
	}
	
	public boolean isMax() {
		return type == ExtremumType.MAX;
	}

	public boolean isMin() {
		return type == ExtremumType.MIN;
	}
	
	public Integer getIndex() {
		return candle.getIndex();
	}
}

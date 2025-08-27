package com.trading.dto;

import com.trading.entity.Candle;
import com.trading.enums.ImpulseType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public  class ImpulseSpan {
	
	public Candle startCandle;
	public Candle endCandle;
	public int startIndex;
	public int endIndex;
	public ImpulseType type;
	public double peakStrength;
//	public Direction direction;
    
}



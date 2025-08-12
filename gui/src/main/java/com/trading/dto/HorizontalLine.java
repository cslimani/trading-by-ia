package com.trading.dto;

import java.awt.Color;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HorizontalLine {

	Double price;
	Color color;
	String type;
	
	public boolean isTypeStopLoss() {
		return "SL".equals(type);
	}
}

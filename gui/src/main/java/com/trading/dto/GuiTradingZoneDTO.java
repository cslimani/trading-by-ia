package com.trading.dto;

import java.awt.Color;
import java.awt.Point;

import lombok.Data;

@Data
public class GuiTradingZoneDTO {

	Long id;
	int xLeft;
	int yBottom;
	int xRight;
	int yTop;
	boolean selected;
	
	public boolean containsPoint(Point point) {
		return point.x >= xLeft && point.x <= xRight && point.y >= yTop && point.y <= yBottom;
	}

	public Color getColor() {
		if (selected) {
			return Color.RED;
		} else {
			return Color.GREEN;
		}
	}
}

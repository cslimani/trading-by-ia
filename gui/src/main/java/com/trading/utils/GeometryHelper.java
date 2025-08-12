package com.trading.utils;

import java.awt.Point;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.trading.dto.GuiLineDTO;
import com.trading.dto.MarketDataDTO;

public class GeometryHelper {


	public static boolean isPointUnderLine(Point p, Point p1, Point p2) {
		return p.getY() >= getLineOrdinate(p.getX(), p1, p2);
	}

	public static boolean isPointAboveLine(Point p, Point p1, Point p2) {
		return p.getY() <= getLineOrdinate(p.getX(), p1, p2);
	}
	
	private static double getLineOrdinate(double x, Point p1, Point p2) {
		double a = getSlope(p1, p2);
		double b = p1.getY() - a*p1.getX();
		return a*x + b;
	}

	public static double getSlope(Point p1, Point p2) {
		return (p2.getY() - p1.getY())/(p2.getX() - p1.getX());
	}

}

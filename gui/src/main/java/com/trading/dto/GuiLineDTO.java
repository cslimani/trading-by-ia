package com.trading.dto;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Point;
import java.time.LocalDateTime;

import com.trading.utils.GeometryHelper;

public class GuiLineDTO {

	private Point p1;
	private Point p2;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private Double startPrice;
	private Double endPrice;
	private int tickness=2;
	private boolean isOverP1;
	private boolean isOverP2;
	private boolean isP1Selected;
	private boolean isP2Selected;
	private String id;
	private Long databaseId;
	private boolean isSelected;
	private GuiLineDTO associatedLine;
	private boolean isDrawn;
	
	public GuiLineDTO(LocalDateTime startDate, Double startPrice, LocalDateTime endDate,  Double endPrice, String id) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.startPrice = startPrice;
		this.endPrice = endPrice;
		this.id = id;
	}

	public boolean isDrawn() {
		return isDrawn;
	}

	public void setDrawn(boolean isDrawn) {
		this.isDrawn = isDrawn;
	}

	public Long getDatabaseId() {
		return databaseId;
	}

	public void setDatabaseId(Long databaseId) {
		this.databaseId = databaseId;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public LocalDateTime getStartDate() {
		return startDate;
	}

	public LocalDateTime getEndDate() {
		return endDate;
	}

	public Double getStartPrice() {
		return startPrice;
	}

	public Double getEndPrice() {
		return endPrice;
	}
	public void onMouseMoved(Point p) {
		if(isOver(p)) {
			tickness = 4;
		} else {
			tickness = 2;
		}
		if (isSelected) {
			tickness = 4;
		}
		isOverP1 = isOverP1(p);
		isOverP2 = isOverP2(p);
	}
	
	public boolean isOver(Point p) {
		if (p1 != null && p2 != null && p.getX() >= p1.getX() && p.getX() <= p2.getX()) {
			if (GeometryHelper.isPointUnderLine(p, new Point(p1.x, p1.y-5), new Point(p2.x, p2.y-5)) &&
					GeometryHelper.isPointAboveLine(p, new Point(p1.x, p1.y+5), new Point(p2.x, p2.y+5))) {
				return true;
			}
		}
		return false;
	}
	public int getTickness() {
		return tickness;
	}
	public Point getP1() {
		return p1;
	}
	public Point getP2() {
		return p2;
	}
	
	public boolean isOverP1(Point point) {
		return isOverPoint(point, p1);
	}
	public boolean isOverP2(Point point) {
		return isOverPoint(point, p2);
	}
	
	public boolean isOverPoint(Point staticPoint, Point movingPoint) {
		if (staticPoint == null || movingPoint == null) {
			return false;
		}
		return Math.sqrt(Math.pow(staticPoint.x - movingPoint.x, 2) + Math.pow(staticPoint.y - movingPoint.y, 2)) < 10;
	}
	public boolean isOverP1() {
		return isOverP1;
	}
	public boolean isOverP2() {
		return isOverP2;
	}
	public boolean isP1Selected() {
		return isP1Selected;
	}
	public void setP1Selected(boolean isP1Selected) {
		this.isP1Selected = isP1Selected;
	}
	public boolean isP2Selected() {
		return isP2Selected;
	}
	public void setP2Selected(boolean isP2Selected) {
		this.isP2Selected = isP2Selected;
	}
	public void clear() {
		isP1Selected = false;
		isP2Selected = false;
	}

	public void setP1(Point p1) {
		this.p1 = p1;
	}

	public void setP2(Point p2) {
		this.p2 = p2;
	}

	public GuiLineDTO getAssociatedLine() {
		return associatedLine;
	}

	public void setAssociatedLine(GuiLineDTO associatedLine) {
		this.associatedLine = associatedLine;
	}

	public Paint getColor() {
		if (startDate.isAfter(endDate)) {
			return Color.RED;
		}
		return Color.WHITE;
	}

}

package com.trading.feature.old;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.trading.dto.GuiTradingZoneDTO;
import com.trading.entity.TradingZone;
import com.trading.gui.MainPanel;
import com.trading.repository.TradingZoneRepository;

import jakarta.transaction.Transactional;
import lombok.Data;

@Component
@Data
public class TradingZoneFeature{

	private static final int KEY_SUPPR = 127;
	boolean isDrawingZone = false;
	TradingZone tradingZone;
	@Autowired
	TradingZoneRepository tradingZoneRepository;
	@Autowired
	MainPanel mainPanel;
	List<GuiTradingZoneDTO> zones;
	boolean dirty = true;

	public void startZone() {
		isDrawingZone = true;
		tradingZone = new TradingZone();
	}

	public void recordDrawingZone(LocalDateTime date, Double price) {
		if (tradingZone.getPriceStart() == null) {
			tradingZone.setDateStart(date);
			tradingZone.setPriceStart(price);
		}
		tradingZone.setDateEnd(date);
		tradingZone.setPriceEnd(price);
	}

	@Transactional
	public void stopDrawing() {
		isDrawingZone = false;
		if (tradingZone.getDateEnd() != null && tradingZone.getDateStart() != null) {
			TradingZone zoneToSave = new TradingZone();
			zoneToSave.setDateStart(tradingZone.getDateStart().isBefore(tradingZone.getDateEnd()) ? tradingZone.getDateStart() : tradingZone.getDateEnd());
			zoneToSave.setDateEnd(tradingZone.getDateStart().isBefore(tradingZone.getDateEnd()) ? tradingZone.getDateEnd() : tradingZone.getDateStart());
			zoneToSave.setPriceStart(Math.min(tradingZone.getPriceStart(), tradingZone.getPriceEnd()));
			zoneToSave.setPriceEnd(Math.max(tradingZone.getPriceStart(), tradingZone.getPriceEnd()));
			tradingZoneRepository.save(zoneToSave);
		} else {
			System.out.println("One of the date is null");
		}
		tradingZone = null;
		reloadZones();
	}

	List<TradingZone> getFromDateRange(LocalDateTime dateStart, LocalDateTime dateEnd){
		List<TradingZone> startList = new ArrayList<>(tradingZoneRepository.findByDateStartBetween(dateStart, dateEnd));
		startList.addAll(tradingZoneRepository.findByDateEndBetween(dateStart, dateEnd));
		return startList;
	}

	private void drawTradingZone(Graphics g, GuiTradingZoneDTO zone) {
//		g.setColor(zone.getColor());
//		int height = zone.getYBottom() - zone.getYTop() ;
//		int width = zone.getXRight() - zone.getXLeft();
//		g.drawRect(zone.getXLeft(), zone.getYTop(), width, height);
	}

	private void reloadZones(){
//		zones = getFromDateRange(mainPanel.data.getFirstGraphDate().getDate(), mainPanel.data.getLastGraphDate().getDate()).stream().map(zone -> {
//			GuiTradingZoneDTO guiZone = new GuiTradingZoneDTO();
//			guiZone.setXLeft(mainPanel.getGraphDateFromDate(zone.getDateStart()).getStartPosition());
//			guiZone.setXRight(mainPanel.getGraphDateFromDate(zone.getDateEnd()).getStartPosition());
// 			guiZone.setYBottom( mainPanel.priceToYPixel(zone.getPriceStart()));
//			guiZone.setYTop( mainPanel.priceToYPixel(zone.getPriceEnd()));
//			guiZone.setId(zone.getId());
//			return guiZone;
//		}).toList();
	}

	public void drawTradingZones(Graphics2D g2d) {
//		if (dirty) {
//			reloadZones();
//			dirty = false;
//		}
//		zones.forEach(zone -> drawTradingZone(g2d, zone));
	}

	public void onMouseClick(Point point) {
//		zones.forEach(zone -> {
//			zone.setSelected(false);
//			if (zone.containsPoint(point)) {
//				zone.setSelected(true);
//			}
//		});
	}

	public void dirty() {
		dirty = true;		
	}

	@Transactional
	public void onPressKey(int keyCode) {
//		if (keyCode == KEY_SUPPR) {
//			zones.forEach(zone -> {
//				if (zone.isSelected()) {
//					Optional<TradingZone> zoneOptional = tradingZoneRepository.findById(zone.getId());
//					if (zoneOptional.isPresent()) {
//						tradingZoneRepository.delete(zoneOptional.get());
//					}
//					dirty();
//				}
//			});
//		}
	}
}

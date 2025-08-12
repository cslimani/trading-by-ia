package com.trading.component;

import java.awt.Color;
import java.awt.Graphics2D;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.trading.entity.Alert;
import com.trading.enums.EnumAlertType;
import com.trading.feature.AbstractFeature;

import jakarta.transaction.Transactional;

@Component
public class AlertComponent extends AbstractFeature{

	boolean selectionLine = false;
	boolean selectionZone = false;
	Double zonePrice = null;
	LocalDateTime zoneDate = null;

	public void draw(Graphics2D g2d) {
		data.getAlerts()
		.stream()
		.filter(a -> a.getType() == EnumAlertType.LINE)
		.forEach(a -> {
			g2d.setColor(Color.ORANGE);
			int y = mainPanel.priceToYPixel(a.getPrice());
			g2d.drawLine(0, y, data.getViewTotalWidth(), y);
		});

		data.getAlerts()
		.stream()
		.filter(a -> a.getType() == EnumAlertType.ZONE)
		.forEach(a -> {
			Integer x = mainPanel.dateToPixel(a.getZoneDate());
			if (x == null) {
				x = mainPanel.dateToPixel(data.getCandels().get(0).getDate());
			}
			g2d.setColor(new Color(255, 153, 51, 80));
			int y = mainPanel.priceToYPixel(a.getZonePriceHigh());
			int height = mainPanel.priceToYPixel(a.getZonePriceLow()) - y;
			LocalDateTime lastDate = data.getCandels().get(data.getCandels().size()-1).getDate();
			int width = mainPanel.dateToPixel(lastDate) - x;
			g2d.fillRect(x, y, width, height);
		});
	}

	public void keyPressed(int keyCode) {
		if (!enabled) {
			return;
		}
		// "a"
		//		System.out.println(keyCode);
		if (keyCode == 65) {
			selectionLine = true;
		}
		// "Z"
		if (keyCode == 90) {
			selectionZone = true;
		}
	}

	public void onClick(LocalDateTime date, Double price) {
		if(selectionLine) {
			Alert alert = Alert.builder()
					.dateCreation(LocalDateTime.now())
					.market(data.getMarketCode())
					.price(price)
					.type(EnumAlertType.LINE)
					.build();
			alertRepository.save(alert);
			alertComponent.reloadAlerts();
		}
		if(selectionZone) {
			if (zonePrice == null) {
				zonePrice = price;
				zoneDate = date;
			} else {
				Alert alert = Alert.builder()
						.dateCreation(LocalDateTime.now())
						.market(data.getMarketCode())
						.type(EnumAlertType.ZONE)
						.zoneDate(date)
						.zonePriceHigh(Math.max(zonePrice, price))
						.zonePriceLow(Math.min(zonePrice, price))
						.build();
				alertRepository.save(alert);
				alertComponent.reloadAlerts();
			}
		}
	}

	public void keyReleased(int keyCode) {
		selectionLine = false;		
		selectionZone = false;
		zonePrice = null;
		zoneDate = null;
	}

	public void reloadAlerts() {
		data.setAlerts(alertRepository.findByMarket(data.getMarketCode()));
	}

	@Transactional
	public void clear(String marketCode) {
		alertRepository.deleteAll(alertRepository.findByMarket(marketCode));
		reloadAlerts();
	}

}

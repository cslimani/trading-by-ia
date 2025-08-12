package com.trading.component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.trading.feature.AbstractFeature;

@Component
public class ZoneSelectorComponent extends AbstractFeature{

	private boolean zoneOnGoing;
	private boolean pointOnGoing;
	private LocalDateTime dateStart;
	List<ZoneSelectedListener> listeners = new ArrayList<>();

	public void keyPressed(int keyCode) {
		if (!enabled) {
			return;
		}
//		System.out.println(keyCode);
//		if (keyCode == 69) {
//			pointOnGoing = true;
//		} else 
//		if (keyCode == 16) {
//			// shift
//			pointOnGoing = false;
//			zoneOnGoing = true;
//		}
	}

	public void keyReleased(int keyCode) {
		reset();
	}

	public void onClick(LocalDateTime date, Double price, int button) {
		if (pointOnGoing) {
			listeners.forEach(l -> l.onNewPointSelected(date));
		}
		if (zoneOnGoing) {
			if (dateStart == null) {
				dateStart = date;
				System.out.println("Setting date start");
			} else {
				System.out.println("Calling listeners zone selector");
				listeners.forEach(l -> l.onNewZoneSelected(dateStart, date));
				reset();
			}
		}
	}
	
	public void addListener(ZoneSelectedListener listener) {
		listeners.add(listener);
	}
	
	public void reset() {
		dateStart = null;
		zoneOnGoing = false;
		pointOnGoing = false;
	}
}

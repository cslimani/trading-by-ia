package com.trading.component;

import java.time.LocalDateTime;

public interface ZoneSelectedListener {

	void onNewZoneSelected(LocalDateTime dateStart, LocalDateTime dateEnd);

	void onNewPointSelected(LocalDateTime date);
	
}

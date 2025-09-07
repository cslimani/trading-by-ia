package com.trading.runner;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.trading.entity.Candle;

@Component
public interface Runner {

	void init();

	void postInit();
	
	void loadData();

	void mouseClicked(LocalDateTime date, Double price);

	void keyPressed(int keyCode);
	
	void keyReleased(int keyCode);

	void afterPricesLoaded(List<Candle> list);
}

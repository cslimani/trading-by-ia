package com.trading.runner;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public interface Runner {

	void init();

	void postInit();
	
	void loadData();

	void mouseClicked(LocalDateTime date, Double price);

	void keyPressed(int keyCode);
	
	void keyReleased(int keyCode);

}

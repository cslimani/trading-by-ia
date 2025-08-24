package com.trading.component;

import java.awt.Color;
import java.awt.Graphics2D;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class VerticalLineFeature extends AbstractFeature {

	private boolean selection = false;
	private LocalDateTime date;

	public void draw(Graphics2D g2d) {
		if (date != null && mainPanel.dateToPixel(date) != null) {
			g2d.setColor(Color.WHITE);
			int x = mainPanel.dateToPixel(date);
			g2d.drawLine(x, 0, x, data.getViewHeight());
		}
	}
	
	public void keyPressed(int keyCode) {
		if (!enabled) {
			return;
		}
		//v
		if (keyCode == 86) {
			selection = true;
		}
	}

	public void onClick(LocalDateTime date, Double price) {
		if(selection) {
			this.date = date;
		}
	}

	public void keyReleased(int keyCode) {
		selection = false;		
		if (keyCode == 67) {
			date = null;
		}
	}
}

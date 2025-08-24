package com.trading.component;

import java.awt.Color;
import java.awt.Graphics2D;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class HorizontalLineFeature extends AbstractFeature {

	private boolean selection = false;
	private Double price;

	public void draw(Graphics2D g2d) {
		if (price != null) {
			g2d.setColor(Color.WHITE);
			int y = mainPanel.priceToYPixel(price);
			g2d.drawLine(0, y, data.getViewTotalWidth(), y);
		}
	}

	public void keyPressed(int keyCode) {
		if (!enabled) {
			return;
		}
		//<
		if (keyCode == 153) {
			selection = true;
		}
	}

	public void onClick(LocalDateTime date, Double price) {
		if(selection) {
			this.price = price;
		}
	}

	public void keyReleased(int keyCode) {
		selection = false;	
		// "c"
		if (keyCode == 67) {
			price = null;
		}
	}


}

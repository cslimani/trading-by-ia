package com.trading.component;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.trading.entity.Candle;

@Component
public class RiskRatioFeature extends AbstractFeature{

	boolean recording;
	Candle candleStart = null;
	Candle candleStoploss = null;

	public void mouseMoved(LocalDateTime date, Double price) {
		StringBuilder sb = new StringBuilder();
		if (candleStart != null && candleStoploss != null && price != null) {
			double rr = (price - candleStart.getOpen()) / (candleStart.getOpen() - candleStoploss.getLow());
			topPanel.updateIndicator("RR = " + String.format("%.1f", rr));
		}
	}

	public void afterCandlesLoaded() {
	}

	public void mouseClicked(LocalDateTime date, Double price) {
		if (recording) {
			if (candleStart == null) {
				candleStart = data.getMapCandles().get(date);
			} else if (candleStoploss == null) {
				candleStoploss = data.getMapCandles().get(date);
			}
		}
	}

	public void keyPressed(int keyCode) {
		//		System.out.println(keyCode);
		// T
		if (keyCode == 84) {
			recording = true;
		}
	}

	public void keyReleased(int keyCode) {
		recording = false;	
		candleStart = null;
		candleStoploss = null;
		topPanel.updateIndicator("");
	}

}

package com.trading.component;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.trading.entity.Candle;
import com.trading.indicator.RsiCalculator;

@Component
public class IndicatorFeature extends AbstractFeature{

	RsiCalculator rsiCalculator;

	public void mouseMoved(LocalDateTime date, Double price) {
		StringBuilder sb = new StringBuilder();
		Double rsi = rsiCalculator.get(date);
		if (rsi != null) {
			sb.append("RSI = " + rsi.intValue());
		}
		Candle c = data.getMapCandles().get(date);
		if (c != null) {
			Double volume = c.getVolume();
			if (volume != null) {
				sb.append("  Vol = " + volume.intValue());
			}
		}
		topPanel.updateIndicator(sb.toString());
	}

	public void afterCandlesLoaded() {
		rsiCalculator = new RsiCalculator(data.getCandles(), 14);
	}

}

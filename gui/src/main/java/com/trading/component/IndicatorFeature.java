package com.trading.component;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.trading.entity.Candle;
import com.trading.indicator.AtrCalculator;
import com.trading.indicator.RsiCalculator;
import com.trading.indicator.SlopeLRCalculator;

@Component
public class IndicatorFeature extends AbstractFeature{

	RsiCalculator rsiCalculator;
	AtrCalculator atrCalculator;
	SlopeLRCalculator slopeLRCalculator;
	
	public void mouseMoved(LocalDateTime date, Double price) {
		StringBuilder sb = new StringBuilder();
		Double rsi = rsiCalculator.get(date);
		if (rsi != null) {
			sb.append("RSI = " + rsi.intValue());
		}
		
		Candle c = data.getMapCandles().get(date);
		if (c != null) {
			Double atr = atrCalculator.getATR(c.getIndex());
			if (atr != null) {
				sb.append("    ATR = " + atr.intValue());
			}
			Double slopeLR = slopeLRCalculator.getSlopeLR(c.getIndex());
			if (slopeLR != null) {
				sb.append("    SlopeATR = " + Double.valueOf((slopeLR*100/atr)).intValue());
			}
			Double volume = c.getVolume();
			if (volume != null) {
				sb.append("    Vol = " + volume.intValue());
			}
		}
		topPanel.updateIndicator(sb.toString());
	}

	public void afterCandlesLoaded() {
		rsiCalculator = new RsiCalculator(data.getCandles(), 14);
		atrCalculator = new AtrCalculator(data.getCandles());
		slopeLRCalculator = new SlopeLRCalculator(data.getCandles());
		data.setAtrCalculator(atrCalculator);
	}

}

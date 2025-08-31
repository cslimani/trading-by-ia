package com.trading.component;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.trading.entity.Candle;
import com.trading.indicator.AtrCalculator;
import com.trading.indicator.EmaCalculator;

@Component
public class DistanceFeature extends AbstractFeature{

	EmaCalculator emaCalculator;
	AtrCalculator atrCalculator;
	boolean recording;
	private Candle candleStart;
	private Double priceStart;
	
	public void keyPressed(int keyCode) {
//		System.out.println(keyCode);
		if (keyCode == 68) {
			recording = true;
		}
	}

	public void keyReleased(int keyCode) {
		recording = false;
		candleStart = null;
		priceStart = null;
	}

	public void mouseClicked(LocalDateTime date, Double price) {
		if (recording) {
			candleStart = data.getMapCandles().get(date);
			priceStart = price;
		}
	}

	public void mouseMoved(LocalDateTime date, Double price) {
		Candle candleEnd = data.getMapCandles().get(date);
		if (candleStart != null && candleEnd != null) {
			Double atr = data.getAtrCalculator().getATR(candleEnd.getIndex());
			if (atr == null) {
				atr = Double.MAX_VALUE;
			}
			int delta = candleEnd.getIndex() - candleStart.getIndex();
			topPanel.updateIndicator("width = " + delta + "    price = " +  Double.valueOf(Math.abs(price - priceStart)).intValue()
					+ "   price/ATR = " + String.format("%.1f",  Math.abs(price - priceStart)/atr));
//			Double ampr = AmprCalculator.getAMPR(data.getCandles(),
//					atrCalculator.getMapATR(),
//					emaCalculator.getMapslope(),
//					candleStart.getIndex(), 
//					candleEnd.getIndex());
//			Double consistency = AmprCalculator.getConsistency(emaCalculator.getMapslope(),
//					candleStart.getIndex(), 
//					candleEnd.getIndex());
//			topPanel.updateIndicator( "   consistency = " + consistency);
		}
	}

	public void afterCandlesLoaded() {
		atrCalculator = new AtrCalculator(data.getCandles());
		emaCalculator = new EmaCalculator(data.getCandles(), 20);
	}
}

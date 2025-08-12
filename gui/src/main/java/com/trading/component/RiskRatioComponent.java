package com.trading.component;

import java.awt.Color;
import java.time.LocalDateTime;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Component;

import com.trading.entity.Candel;
import com.trading.entity.TrainingTrade;
import com.trading.feature.AbstractFeature;
import com.trading.utils.AppUtils;

@Component
public class RiskRatioComponent extends AbstractFeature{

	boolean onGoing;
	Double openPrice;
	LocalDateTime openDate;
	Candel candelStoploss;
	private Double total = 0d;

	public void keyPressed(int keyCode) {
		if (!enabled) {
			return;
		}
		//		System.out.println(keyCode);
		// "r"
		if (keyCode == 83 && !onGoing) {
			reset();
			onGoing = true;
		} 
		if (keyCode == 69) {
			onGoing = true;
		} 
	}

	public void keyReleased(int keyCode) {
		onGoing = false;
	}

	public void onClick(LocalDateTime date, Double price, int button) {
		//		System.out.println(button);
		if (onGoing) {
			if (openPrice == null) {
				openPrice = price;
				openDate = date;
				topPanel.setTradeInfo("1/3", Color.YELLOW);
			} else if (candelStoploss == null) {
				candelStoploss = data.getMapCandels().get(date);
				topPanel.setTradeInfo("2/3", Color.ORANGE);
			} else {
				//				Candel candel = data.getMapCandels().get(date);
//				Candel candelMax = getMax(firstCandel, candel);
				//				Double ratio = (candel.getLow() - firstCandel.getLow()) / (candelMax.getHigh() - firstCandel.getLow())*100f;
//				Double ratio = Math.abs(firstCandel.getHigh() - candel.getLow());
//				String text = "Count = " + Math.abs(candel.getIndex() - firstCandel.getIndex()) +
//						" - ratio = " + ratio;
				//				System.out.println(Math.abs(candel.getIndex() - firstCandel.getIndex()));
				Double ratio = getRatio(price);
				if (ratio < -1) {
					ratio = -1d;
				}
				total += ratio;
				createTrade(date, price, ratio);
				String text = "";
				topPanel.setCandelCounter(text);
				reset();
				topPanel.setTradeInfo("3/3", Color.GREEN);
			}
		}
	}

	private void createTrade(LocalDateTime dateClose, Double priceClose, Double riskRatio) {
		TrainingTrade trade = new TrainingTrade();
		trade.setDateOpen(openDate);
		trade.setDateStoploss(candelStoploss.getDate());
		trade.setStoploss(candelStoploss.getLow());
		trade.setDateClose(dateClose);
		trade.setPriceClose(priceClose);
		trade.setMarket(data.getMarketCode());
		trade.setDateCreation(LocalDateTime.now());
		trade.setType("MANUAL");
//		trade.setIdentifier(RandomUtils.nextLong());
		trade.setRiskRatio(riskRatio);
		trainingTradeRepository.save(trade);
		System.out.println("Trade created");
	}

	public void mouseMoved(LocalDateTime date, Double price) {
		if (openPrice != null && candelStoploss != null) {
			topPanel.setCandelCounter("Total " + total + " - RR = " + getRatio(price));
			
		} else {
			topPanel.setCandelCounter("Total " + total);
		}
	}
	
	private Double getRatio(Double price) {
		Double sl = openPrice - candelStoploss.getLow();
		Double tp = price - openPrice;
		return AppUtils.getRoundedNumber(tp / sl, 1);
	}

	private Candel getMax(Candel c1, Candel c2) {
		Candel max = c1;
		for (int i = c1.getIndex(); i <= c2.getIndex(); i++) {
			Candel c = data.getCandels().get(i);
			if (c.getHigh() > max.getHigh()) {
				max = c;
			}
		}
		return max;
	}

	public void reset() {
		onGoing = false;
		openPrice = null;
		candelStoploss = null;
	}

	
}

package com.trading.feature;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class RiskRatioFeature extends AbstractFeature{
	
	private Double price1;
	private Double price2;
	
	public void onClick(LocalDateTime date, Double price, int button) {
//		 if (button == 3){
//			if (price1 == null) { 
//				price1 = price;
//			} else if (price2 == null){
//				price2 = price;
//			} else {
//				Double price3 =  price;
//				Double stoploss = price2 - price1;
//				Double tp = price1 - price3;
//				topPanel.showDiff(tp/stoploss);
//				price1 = null;
//				price2 = null;
//			}
//		}
		
	}

}

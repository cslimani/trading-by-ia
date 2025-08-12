package com.trading.component;

import java.awt.Color;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.trading.dto.HorizontalLine;
import com.trading.entity.Candel;
import com.trading.entity.TrainingTrade;
import com.trading.enums.EnumDirection;
import com.trading.feature.AbstractFeature;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BacktestComponent extends AbstractFeature{

	boolean buy = false;
	boolean sell = false;
	TrainingTrade trainingTrade;

	public void keyPressed(int keyCode) {
		if (!enabled) {
			return;
		}
		// "B"
		if (keyCode == 66 ) {
			buy = true;
		}

		// "S"
		if (keyCode == 83 ) {
			sell = true;
		}

		// "c"
		if (keyCode == 67 && trainingTrade != null) {
			saveTrade();
		}
	}

	private void saveTrade() {
		Candel lastCandel = data.getCandels().get(data.getCandels().size()-1);
		trainingTrade.setDateClose(lastCandel.getDate());
		trainingTrade.setPriceClose(lastCandel.getCloseBid());
		if (trainingTrade.getDirection() == EnumDirection.BUY) {
			trainingTrade.setRiskRatio(trainingTrade.buildRiskRatio(lastCandel.getCloseBid()));
		} else {
			trainingTrade.setRiskRatio(trainingTrade.buildRiskRatio(lastCandel.getCloseAsk()));
		}
		trainingTradeRepository.save(trainingTrade);
		data.getPricesToDraw().clear();
		trainingTrade = null;
		log.info("Trade created");
	}

	public void onClick(LocalDateTime date, Double price) {
		if (buy) {
			data.getPricesToDraw().clear();
			Candel lastCandel = data.getCandels().get(data.getCandels().size()-1);
			Candel candelStoploss = data.getMapCandels().get(date);
			if (candelStoploss.getLowBid() != null && lastCandel.getCloseAsk() != null) {
				trainingTrade = createTrade(EnumDirection.BUY,
						lastCandel.getCloseAsk(),
						lastCandel.getDate(),
						candelStoploss.getDate(), 
						candelStoploss.getLowBid());
				data.getPricesToDraw().add(new HorizontalLine(trainingTrade.getPriceOpen(), Color.ORANGE, "OPEN"));
				data.getPricesToDraw().add(new HorizontalLine(trainingTrade.getStoploss(), Color.RED, "SL"));
			}
		} 
		if (sell) {
			data.getPricesToDraw().clear();
			Candel lastCandel = data.getCandels().get(data.getCandels().size()-1);
			Candel candelStoploss = data.getMapCandels().get(date);
			if (candelStoploss.getHighAsk() != null && lastCandel.getCloseBid() != null) {
				trainingTrade = createTrade(EnumDirection.SELL,
						lastCandel.getCloseBid(),
						lastCandel.getDate(), 
						candelStoploss.getDate(),
						candelStoploss.getHighAsk());
				data.getPricesToDraw().add(new HorizontalLine(trainingTrade.getPriceOpen(), Color.ORANGE, "OPEN"));
				data.getPricesToDraw().add(new HorizontalLine(trainingTrade.getStoploss(), Color.RED, "SL"));
				if (trainingTrade.getDirection() == EnumDirection.SELL) {
					data.getPricesToDraw().add(new HorizontalLine(trainingTrade.getPriceOpen(), Color.WHITE, "ASK"));
				}
			}
		} 
	}

	private TrainingTrade createTrade(EnumDirection direction, Double openPrice, LocalDateTime openDate, LocalDateTime dateStoploss, Double priceStoploss) {
		TrainingTrade trade = new TrainingTrade();
		trade.setDirection(direction);
		trade.setDateOpen(openDate);
		trade.setPriceOpen(openPrice);
		trade.setDateStoploss(dateStoploss);
		trade.setStoploss(priceStoploss);
		trade.setMarket(data.getMarketCode());
		trade.setDateCreation(LocalDateTime.now());
		return trade;
	}

	public void keyReleased(int keyCode) {
		buy = false;
		sell = false;
	}

	public void updateTradeInfo() {
		if (trainingTrade != null) {
			Candel lastCandel = data.getCandels().get(data.getCandels().size()-1);
			if (trainingTrade.getDirection() == EnumDirection.BUY) {
				topPanel.setTradeInfo("RR = " + trainingTrade.buildRiskRatio(lastCandel.getCloseBid()), Color.WHITE);
			} else {
				topPanel.setTradeInfo("RR = " + trainingTrade.buildRiskRatio(lastCandel.getCloseAsk()), Color.WHITE);
				data.getPricesToDraw().stream().filter(line -> line.getType().equals("ASK")).forEach(line -> line.setPrice(lastCandel.getCloseAsk()));
			}

		}
	}

}

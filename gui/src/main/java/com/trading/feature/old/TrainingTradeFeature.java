package com.trading.feature.old;

import java.awt.Color;
import java.time.LocalDateTime;
import java.util.List;

import javax.swing.JTextArea;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.trading.entity.Candel;
import com.trading.entity.TrainingTrade;
import com.trading.enums.EnumTimeRange;
import com.trading.feature.AbstractFeature;
import com.trading.repository.TrainingTradeRepository;

@Component
public class TrainingTradeFeature extends AbstractFeature {

	@Autowired
	TrainingTradeRepository trainingTradeRepository;
	int index = 0;
	List<TrainingTrade> trades;
	private TrainingTrade trade;
	private Double stoploss = null;
	private JTextArea tradeInfoArea;
	private Candel openCandel;
	private List<Candel> candels;
	private Candel closeCandel;
	private Candel selectedCandel;
	int nbCandels = 100;

	public void init() {
//		trades =  Lists.newArrayList(trainingTradeRepository.findAllByOrderByPredictionDesc());
//		addTradeInfoArea();
//		addButton("Next", () -> nextTrade(), true);
//		addButton("SL", () ->{
//			trade.setReason("STOPLOSS");
//			touchPriceAction(stoploss, trade.getDateOpen());
//		}, false);
//		
//		addButton("Break Even", () -> {
//			trade.setReason("BREAK_EVEN");
//			if (selectedCandel != null) {
//				touchPriceAction(openCandel.getOpen(), selectedCandel.getDate());	
//			} else {
//				touchPriceAction(openCandel.getOpen(), trade.getDateOpen());				
//			}
//		},false);
		
		addButton("Reset", () -> reset(), true);
		addButton("More Candels", () -> nbCandels += 100, true);
		addButton("Delete", () -> deleteTrade(), true);
	}


	private void deleteTrade() {
		transactionnal(() -> {
			TrainingTrade t = trainingTradeRepository.save(trade);
			trainingTradeRepository.delete(t);
		});
		index++;
		reset();
		mainPanel.reload();
	}


	private void reset() {
		nbCandels = 100;
		stoploss = null;
		selectedCandel = null;
		if (closeCandel != null) closeCandel.setSelected(false);
		closeCandel = null;
//		System.out.println("Reset data");
	}



	private void touchPriceAction(Double price, LocalDateTime dateAfter) {
		if (price != null) {
			boolean priceTouched = false;
			for (Candel c : candels) {
				if (c.getDate().isAfter(dateAfter)) {
					if (c.getOpen() < price && c.getHigh() >= price || c.getOpen() > price && c.getLow() <= price) {
						if (closeCandel != null) closeCandel.setSelected(false); 
						closeCandel = c;
						closeCandel.setSelected(true);
						System.out.println("Close candel will be " + c.getDate());
						mainPanel.repaint();
						priceTouched = true;
						break;
					}
				}
			};
			if (!priceTouched) {
				topPanel.info("Price never touched !!!!!!!!!!!!!!!!!", Color.red);
			}
		}
	}

	private void addTradeInfoArea() {
		tradeInfoArea = new JTextArea();
		topPanel.add(tradeInfoArea);
		tradeInfoArea.setLocation(20, 20);
	}

	private void nextTrade() {
		index++;
		reset();
		mainPanel.reload();
		
//		if (closeCandel != null) {
//			System.out.println("Saving current trade");
//			transactionnal(() -> {
//				TrainingTrade t = trainingTradeRepository.save(trade);
//				t.setDateClose(closeCandel.getDate());
//				t.setStoploss(stoploss);
//				t.setReason(trade.getReason());
//				t.setDateStoploss(trade.getDateStoploss());
//				trainingTradeRepository.save(t);
//			});
//			index++;
//			reset();
//			mainPanel.reload();
//		} else {
//			topPanel.info("Select a close candel to go next", Color.red);
//		}
	}

	public void loadData() {
//		trade = trades.get(index);
////		System.out.println("Loading data for trade " + trade.getDateOpen());
//		System.out.println("Prediction " + trade.getPrediction());
//		candels = candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(trade.getMarket(),EnumTimeRange.S15, trade.getDateMax(), trade.getDateOpen());
//		candels.forEach(c -> {
//			if (trade.getDateOpen().isEqual(c.getDate())) {
//				openCandel = c;
//				c.setSelected(true);
//			}
//		});
	}

	public List<Candel> getCandels() {
		return candels;
	}

	public void onClick(LocalDateTime date, Double price, int button) {
		if (stoploss == null) {
			if (price < openCandel.getOpen()) {
				warn("SL not valid");
				return;
			}
			stoploss = price;
			trade.setDateStoploss(date);
			topPanel.info("SL set");
		} else if (button == 3) {
//			trade.setReason("TP");
			touchPriceAction(price, date);
		} else {
			candels.forEach(c -> {
				if (date.isEqual(c.getDate())) {
					selectedCandel = c;
					System.out.println("Selected candel at " + c.getDate());
				}
			});
		}
	}

	public void mouseMoved(LocalDateTime date, Double price) {
		if (stoploss != null) {
			Double slDistance = stoploss - openCandel.getOpen();
			Double tpDistance = openCandel.getOpen() - price;
			Double rr = tpDistance/slDistance;
			tradeInfoArea.setText("RR : " + rr.toString());
		}
	}


}

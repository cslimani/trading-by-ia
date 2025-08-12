package com.trading.feature.old;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.trading.entity.Candel;
import com.trading.entity.Trade;
import com.trading.entity.TrainingTrade;
import com.trading.feature.AbstractFeature;

import jakarta.annotation.PostConstruct;

@Component
public class TradeVisualizationFeature extends AbstractFeature{
	
	int index = 0;
	List<TrainingTrade> trades;
	private Double price1;
	private Double price2;
	
	@PostConstruct
	public void init() {
//		data.setTrades(loadTrades());
	}
	
	private List<Trade> loadTrades(){
		Path filePath = Paths.get("/home/cyril/data/beta5/trade_entries_44");
		List<Trade> trades = null;
		try {
			trades = Files.lines(filePath).toList().parallelStream().map(line -> createTrade(line)).toList();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return trades;
	}
	
	private Trade createTrade(String line) {
		try {
			Trade trade = objectMapper.readValue(line, Trade.class);
			return trade;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public List<Candel> getCandelsForTrades() {
		Trade trade = data.getTrades().get(data.getIndexTrade());
		data.setTrade(trade);
//		LocalDateTime startDate = trade.getLastMax().getDate();
//		LocalDateTime endDate = trade.getOpenCandel().getDate().plusMinutes(300);
		List<Candel> candels =  null; //candelRepository.findByMarketAndDateBetweenOrderByDateAsc(trade.getMarket() ,startDate, endDate);
//		candels.forEach(c -> {
//			if (c.getDate().isEqual(trade.getOpenCandel().getDate())) {
//				c.setSelected(true);
//			}
//		});
		return candels;
	}

	public void onClick(LocalDateTime date, Double price, int button) {
		 if (button == 2){
			if (price1 == null) { 
				price1 = price;
			} else if (price2 == null){
				price2 = price;
			} else {
				Double price3 =  price;
				Double stoploss = price2 - price1;
				Double tp = price1 - price3;
				topPanel.showDiff(tp/stoploss, 0d);
				price1 = null;
				price2 = null;
			}
		}
		
	}

	public void mouseMoved(LocalDateTime date, Double price) {
		// TODO Auto-generated method stub
		
	}
}

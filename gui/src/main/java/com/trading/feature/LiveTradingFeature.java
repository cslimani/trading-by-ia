package com.trading.feature;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.trading.dto.HorizontalLine;
import com.trading.entity.Accumulation;
import com.trading.entity.Candel;
import com.trading.entity.Trade;
import com.trading.enums.EnumDirection;
import com.trading.enums.EnumTimeRange;
import com.trading.enums.EnumTradeStatus;
import com.trading.utils.AppUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LiveTradingFeature extends AbstractFeature implements PriceUpdateListener{

	private static final double RISK_PER_TRADE_EURO = 2d;
	private List<Candel> candels;
	private Trade trade;
	private boolean enableTakeProfit = false;
	private boolean enableStoploss= false;
	private String currentMarket = null;
	private List<Trade> openTrades = List.of();

	public void init() {
		setEnabled(true);
		priceStreamingClient.connect();
		priceHolderService.init();
		priceHolderService.addListener(this);
		tradeService.checkTradeStatusFromBroker();
		tradeService.fetchOpenTradesInfos();
		alertComponent.setEnabled(true);
		addButton("Expand", () -> stretchZone(30), true);
		addButton("Buy", () -> buyTrade(), true);
		addButton("Sell", () -> sellTrade(), true);
		addButton("Close", () -> closeTrade(), true);
		addButton("Take profit", () -> switchTakeProfit(), true);
		addButton("Stoploss", () -> switchStoploss(), true);
		addButton("Sync trades", () -> syncTradeInfoWithRemote(), true);
		addButton("Clear Acc", () -> clearAcc(), true);
		addButton("Clear alerts", () -> alertComponent.clear(data.getMarketCode()), true);
		showTradeInfos();
	}

	private void clearAcc() {
		List<Accumulation> accs = accumulationRespository.findByMarketAndStatus(data.getMarketCode(), "NEW");
		accs.forEach(acc -> {
			acc.setStatus("SEEN");
			accumulationRespository.save(acc);
		});
		data.getMarket().setAlert(false);
		checkAccumulationsAndAlerts();
	}

	private void syncTradeInfoWithRemote() {
		tradeService.checkTradeStatusFromBroker();
		tradeService.fetchOpenTradesInfos();
		showTradeInfos();
	}

	private void switchStoploss() {
		enableStoploss = !enableStoploss;
		log.info("SL is {}", enableStoploss);
	}

	private void switchTakeProfit() {
		enableTakeProfit = !enableTakeProfit;
		log.info("TP is {}", enableTakeProfit);
	}

	private void closeTrade() {
		if (trade != null) {
			tradeService.closeTrade(trade);
			tradeService.checkTradeStatusFromBroker();
			showTradeInfos();
			refresh();
		}
	}

	private synchronized void showTradeInfos() {
		System.out.println("Updating trades infos");
		openTrades = tradeRepository.findByStatus(EnumTradeStatus.OPEN);
		data.getMapMarkets().values().forEach(m -> m.setOpenTrade(false));
		openTrades.forEach(t -> {
			data.getMapMarkets().get(t.getMarket()).setOpenTrade(true);
		});
		List<Trade> tradesForMarket = tradeRepository.findByMarketAndStatus(data.getMarketCode(), EnumTradeStatus.OPEN);
		data.getPricesToDraw().clear();
		if (!tradesForMarket.isEmpty()) {
			trade = tradesForMarket.get(0);
			if (trade.getStoploss() != null) {
				data.getPricesToDraw().add(new HorizontalLine(trade.getStoploss(), Color.RED, ""));
			}
			data.getPricesToDraw().add(new HorizontalLine(trade.getPriceOpen(), Color.WHITE, ""));
			if (trade.getTakeProfit() != null) {
				data.getPricesToDraw().add(new HorizontalLine(trade.getTakeProfit(), Color.GREEN, ""));
			}
		} else {
			trade = null;
		}
		leftPanel.updateButtons();
	}

	private void sellTrade() {
		Candel max = AppUtils.getMax(candels.size()-50, candels.size()-1, candels);
		//		log.info("Min is at {}", min.getDate());
		Double stoploss = max.getHighAsk();
		Candel lastCandel = candels.get(candels.size()-1);
		Double openPrice = lastCandel.getCloseAsk();
		if (openPrice == null) {
			log.info("Last ask price is null, not opening trade");
			return;
		}
		double volume = AppUtils.getRoundedNumber(RISK_PER_TRADE_EURO/(data.getMarket().getScale()*(stoploss - openPrice)), 2);
		volume = Math.max(volume, 0.01d);
		log.info("Trying to open SELL trade with openPrice={} - stoploss={} - volume={}", openPrice, stoploss, volume);
		Trade trade = Trade.builder()
				.dateCreation(LocalDateTime.now())
				.direction(EnumDirection.SELL)
				.market(data.getMarketCode())
				.stoploss(stoploss)
				.originalStoploss(stoploss)
				.market(data.getMarketCode())
				.riskEuro(RISK_PER_TRADE_EURO)
				.status(EnumTradeStatus.PENDING)
				.volume(volume)
				.build();
		try {
			tradeService.openTrade(trade);
			tradeService.fetchOpenTradesInfos();
			showTradeInfos();
		} catch (IOException e) {
			log.error("Error creating trade", e);
		}
	}

	private void buyTrade() {
		Candel min = AppUtils.getMin(candels.size()-50, candels.size()-1, candels);
		//		log.info("Min is at {}", min.getDate());
		Double stoploss = min.getLowBid();
		Candel lastCandel = candels.get(candels.size()-1);
		Double openPrice = lastCandel.getCloseAsk();
		if (openPrice == null) {
			openPrice = lastCandel.getCloseBid();
		}
		double volume = AppUtils.getRoundedNumber(RISK_PER_TRADE_EURO/(data.getMarket().getScale()*(openPrice - stoploss)), 2);
		volume = Math.max(volume, 0.01d);
		log.info("Trying to open trade with openPrice={} - stoploss={} - volume={}", openPrice, stoploss, volume);
		Trade trade = Trade.builder()
				.dateCreation(LocalDateTime.now())
				.direction(EnumDirection.BUY)
				.market(data.getMarketCode())
				.stoploss(stoploss)
				.originalStoploss(stoploss)
				.market(data.getMarketCode())
				.riskEuro(RISK_PER_TRADE_EURO)
				.status(EnumTradeStatus.PENDING)
				.volume(volume)
				.build();
		try {
			tradeService.openTrade(trade);
			tradeService.fetchOpenTradesInfos();
			showTradeInfos();
		} catch (IOException e) {
			log.error("Error creating trade", e);
		}
	}

	public void refresh() {
		mainPanel.reload(false);		
	}

	public void loadData() {
		EnumTimeRange timeRange = data.getTimeRange();
		if (timeRange == null) {
			throw new RuntimeException("No timerange");
		}
		if (data.getDirection() == null) {
			data.setDirection(EnumDirection.BUY.name());
		}

		candels = priceHolderService.getCandels(data.getMarketCode(), data.getTimeRange());

		candels.stream().forEach(c -> c.setColor(null));
		showAccumulationInfo();
		if (currentMarket == null || !currentMarket.equals(data.getMarketCode())) {
			currentMarket = data.getMarketCode();
			showTradeInfos();
			alertComponent.reloadAlerts();
			checkAccumulationsAndAlerts();
		}
		data.setCandels(candels);
	}

	private void showAccumulationInfo() {
		Optional<Accumulation> accOpt = data.getAccumulations()
				.stream()
				.filter(acc -> acc.getMarket().equals(data.getMarketCode()))
				.findFirst();
		if (accOpt.isPresent()) {
			candels.stream().filter(c -> c.getDate().equals(accOpt.get().getDateEnd())).forEach(c -> c.setColor(Color.WHITE));
			candels.stream().filter(c -> c.getDate().equals(accOpt.get().getDateStart())).forEach(c -> c.setColor(Color.WHITE));
		}
	}

	public void stretchZone(int value) {
		mainPanel.unzoomMaximum();
	}

	public List<Candel> getCandels() {
		return candels;
	}

	public void keyReleased(int keyCode) {
	}

	public void keyPressed(int keyCode) {
		//ctrl
		if (keyCode == 17) {
		}
	}

	public void onClick(LocalDateTime date, Double price) {
		if (enableTakeProfit && trade != null) {
			trade.setTakeProfit(price);
			boolean success = tradeService.modifyTrade(trade);
			if (success) {
				switchTakeProfit();
				topPanel.setTradeInfo("TP OK", Color.GREEN);
				waitFor(2000);
				tradeService.fetchOpenTradesInfos();
			} else {
				topPanel.setTradeInfo("Error setting TP", Color.RED);
			}
			showTradeInfos();
		}
		if (enableStoploss && trade != null) {
			trade.setStoploss(price);
			boolean success = tradeService.modifyTrade(trade);
			if (success) {
				waitFor(2000);
				switchStoploss();
				topPanel.setTradeInfo("SL OK", Color.GREEN);
				tradeService.fetchOpenTradesInfos();
			} else {
				topPanel.setTradeInfo("Error setting TP", Color.RED);
			}
			showTradeInfos();
		}
	}

	private void waitFor(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			log.error("Error sleeping", e);
		}
	}

	public void mouseMoved(LocalDateTime date, Double price) {
		//		System.out.println(price);
	}


	@Override
	public void onUpdatePrice(Candel candel, EnumTimeRange tr) {
		log.debug(candel.getMarket() +  " - New price");
		if (tr == data.getTimeRange() && candels != null && !candels.isEmpty() && candel.getMarket().equals(data.getMarketCode())) {
			synchronized (tr) {
				Candel lastCandel = candels.get(candels.size()-1);
				if (lastCandel.getDate().isEqual(candel.getDate())) {
					//Bid
					lastCandel.setLowBid(candel.getLowBid());
					lastCandel.setHighBid(candel.getHighBid());
					lastCandel.setOpenBid(candel.getOpenBid());
					lastCandel.setCloseBid(candel.getCloseBid());
					//				System.out.println("Updating candel with close bid " + candel.getCloseBid());

					//Ask
					lastCandel.setLowAsk(candel.getLowAsk());
					lastCandel.setHighAsk(candel.getHighAsk());
					lastCandel.setOpenAsk(candel.getOpenAsk());
					lastCandel.setCloseAsk(candel.getCloseAsk());
				} else {
					candels.add(candel);
					//				System.out.println("New candel with close bid " + candel.getCloseBid());
				}
				try {
					mainPanel.reload(false);	
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		}
		checkTradeStatus(candel);
	}

	private void checkTradeStatus(Candel candel) {
		if (candel.getTimeRange() != EnumTimeRange.S15) {
			return;
		}
		openTrades
		.stream()
		.filter(t -> t.getMarket().equals(candel.getMarket()))
		.forEach(trade -> {
			if (trade.getPriceOpen() != null && data.getMapMarkets().get(trade.getMarket()) != null) {
				data.getMapMarkets().get(trade.getMarket()).setRiskRatio(AppUtils.getRoundedNumber(trade.buildRiskRatio(candel.getClose()), 1));
				leftPanel.updateButtons();
			}
			if (trade.getDirection() == EnumDirection.BUY) {
				if (candel.getLow() <= trade.getStoploss()) {
					tradeService.checkTradeStatusFromBroker();
					showTradeInfos();
				}
				if (trade.getTakeProfit() != null && candel.getHigh() >= trade.getTakeProfit()) {
					tradeService.checkTradeStatusFromBroker();
					showTradeInfos();
				}
			}
			if (trade.getDirection() == EnumDirection.SELL) {
				if(candel.getHigh() >= trade.getStoploss()) {
					tradeService.checkTradeStatusFromBroker();
					showTradeInfos();
				}
				if (trade.getTakeProfit() != null && candel.getLow() <= trade.getTakeProfit()) {
					tradeService.checkTradeStatusFromBroker();
					showTradeInfos();
				}
			}
		});
	}

	public synchronized void checkAccumulationsAndAlerts() {
		data.getMapMarkets().values().forEach(m -> m.setAlert(false));

		List<Accumulation> accumulations = accumulationRespository.findByStatusAndDateEndAfter("NEW", LocalDateTime.now().minusMinutes(30));
		accumulations.forEach(acc -> {
			data.getMapMarkets().get(acc.getMarket()).setAlert(true);
			log.debug("Acc found for " + acc.getMarket());
		});
		data.setAccumulations(accumulations);

		alertRepository.findBySent(true).forEach(a -> {
			data.getMapMarkets().get(a.getMarket()).setAlert(true);
		});

		leftPanel.updateButtons();
	}


}

package com.trading.feature.old;

import java.awt.Color;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.trading.dto.HorizontalLine;
import com.trading.entity.Candel;
import com.trading.entity.TrainingTrade;
import com.trading.feature.AbstractFeature;
import com.trading.feature.PriceAnalysisFeature;

import jakarta.transaction.Transactional;

@Component
public class BacktestFeatureOld extends AbstractFeature{

	private static final Double EXTRA_STOPLOSS = 0d;
	Candel candelOpen;
	Double priceOpen;
	Candel candelStoploss;
	Double spread = 0d;
	boolean enabled;
	private Candel lastCandel;
	Double bestRiskRatio = 0d;
	private String timerangeOpen;

	@Autowired
	PriceAnalysisFeature priceAnalysisFeature;

	public void init() {
		addButton("Open", () -> openTrade(), true);
		addButton("Close", () -> closeTrade(), true);
		data.setTrainingTrades(trainingTradeRepository.findByMarket(data.getMarket().getCode()));
	}
	
	@Transactional
	public void onClick(LocalDateTime date, Double price) {
		if (enabled) {
			Candel candel = data.getMapCandels().get(date);
			if (candelOpen == null) {
				candelOpen = candel;
				priceOpen = price;
				topPanel.setTradeInfo("Waiting for SL", Color.RED);
				timerangeOpen = data.getTimeRange().name();
				data.getPricesToDraw().add(new HorizontalLine(price, Color.ORANGE, "OPEN"));
			} else if (candelStoploss == null) {
				candelStoploss = candel;
				topPanel.setTradeInfo("2/3", Color.WHITE);
				if(data.getDirection().equals("BUY")) {
					data.getPricesToDraw().add(new HorizontalLine(candelStoploss.getLow() - EXTRA_STOPLOSS, Color.RED, "SL"));
				} else {
					data.getPricesToDraw().add(new HorizontalLine(candelStoploss.getHigh() + EXTRA_STOPLOSS, Color.RED, "SL"));
				}
//				findBestResult();
				//				plotTicks(candelOpen.getDate(), date);
			} else {
				closeTrade(price, date);
			}
		}
	}


	private void plotTicks(LocalDateTime startDate, LocalDateTime endDate) {
		Thread t = new Thread(() -> {
			List<Double> ticks = tickPriceRepository.findByMarketAndDateBetweenOrderByDate("DE30", startDate, endDate).stream()
					.map(tick -> tick.getAsk()).toList();
			Double maxDrawdown = getMaxDrawdown(ticks);
			topPanel.showDiff(0d, maxDrawdown);
			plot(ticks);
		});
		t.start();
	}

	private Double getMaxDrawdown(List<Double> ticks) {
		Double maxDrawdown = 0d;
		double maxTotal = ticks.stream().mapToDouble(Double::doubleValue).max().getAsDouble();
		double minTotal = ticks.stream().mapToDouble(Double::doubleValue).min().getAsDouble();
		double currentMin = ticks.get(0);
		for (Double price : ticks) {
			if (price < currentMin) {
				currentMin = price;
			}
			double drawdown = (price - currentMin)/(maxTotal - minTotal);
			maxDrawdown = Math.max(maxDrawdown, drawdown);
		}
		return maxDrawdown*100;
	}

	private void findBestResult() {
		bestRiskRatio = 0d;
		if (candelStoploss != null && priceOpen != null) {
			Candel bestCandel = null;
			List<Candel> candels = candelRepository.findByMarketAndTimeRangeAndDateAfterOrderByDateAsc(
					data.getMarket().getCode(), data.getTimeRange(), candelOpen.getDate(), PageRequest.of(0, 10000));
			for (Candel candel : candels) {
				if (candel.getDate().isAfter(candelOpen.getDate())) {
					if (data.getDirection().equals("BUY")) {
						Double riskRatio = calculateRR(candel.getLow());
						if (riskRatio <= -1) {
							break;
						} else {
							bestRiskRatio = Math.max(bestRiskRatio, calculateRR(candel.getHigh()));
							if (bestCandel == null || bestCandel.getHigh() < candel.getHigh()) {
								bestCandel = candel;
							}
						}
					} else {
//						if(candel.isDate(2016, 5, 3, 15, 33, 15)) {
//							System.out.println();
//						}
						Double riskRatio = calculateRR(candel.getHigh());
						if (riskRatio <= -1) {
							break;
						} else {
							bestRiskRatio = Math.max(bestRiskRatio, calculateRR(candel.getLow()));
							if (bestCandel == null || bestCandel.getLow() > candel.getLow()) {
								bestCandel = candel;
							}
						}
					}
				}
			}
			if (bestCandel == null) {
				bestCandel = candels.get(0);
			}
			if (data.getDirection().equals("BUY")) {
				closeTrade(bestCandel.getHigh(), bestCandel.getDate());
			} else {
				closeTrade(bestCandel.getLow(), bestCandel.getDate());
			}
			topPanel.showDiff(0d, bestRiskRatio);
		}
	}

	private void openTrade() {
		Candel c = data.getCandels().get(data.getCandels().size()-1);
		enabled = true;
		onClick(c.getDate(), c.getClose());
		enabled = false;
	}

	private void closeTrade() {
		Candel c = data.getCandels().get(data.getCandels().size()-1);
		closeTrade(c.getClose(), c.getDate());
	}
	
	private void closeTrade(Double price, LocalDateTime date) {
		if (candelStoploss == null) {
			System.out.println("Can not close trade because no stoploss !");
			return;
		}
		TrainingTrade trade = new TrainingTrade();
		trade.setDateOpen(candelOpen.getDate());
		trade.setDateStoploss(candelStoploss.getDate());
		if (data.getDirection().equals("BUY")) {
			trade.setStoploss(candelStoploss.getLow());
		} else {
			trade.setStoploss(candelStoploss.getHigh());
		}
		trade.setDateClose(date);
		trade.setPriceClose(price);
		trade.setMarket(data.getMarket().getCode());
		trade.setDateCreation(LocalDateTime.now());
		trade.setType(data.getTradeType());
//		trade.setIdentifier(RandomUtils.nextLong());
		Double riskRatio = calculateRR(price);
		topPanel.showDiff(riskRatio, bestRiskRatio);
		trade.setRiskRatio(riskRatio);
//		trade.setTimerangeOpen(timerangeOpen);
//		trade.setTimerangeClose(data.getTimeRange().name());
		topPanel.setTradeInfo("3/3", Color.GREEN);
		trade = trainingTradeRepository.save(trade);
		reset();
		priceAnalysisFeature.onCloseTrade(trade);
		System.out.println("Trade closed with rr = " + riskRatio);
	}

	private Double calculateRR(Double price) {
		Double rr = 0d;
		if (data.getDirection().equals("BUY")) {
			rr = (price - spread - priceOpen) / (priceOpen - (candelStoploss.getLow() - EXTRA_STOPLOSS - spread));
		} else {
			rr = (priceOpen  - (price + spread)) / (candelStoploss.getHigh() + EXTRA_STOPLOSS +  spread - priceOpen);
		}
		if (rr < -1) {
			rr = -1d;
		}
		return rr;
	}

	public void keyPressed(int keyCode) {
		//				System.out.println(keyCode);
		//ctrl
		if (keyCode == 17) {
			enabled = true;
		}
	}

	@Transactional
	public void keyReleased(int keyCode) {
		//d
		if (keyCode == 68) {
			TrainingTrade trade = trainingTradeRepository.findFirstByOrderByDateCreationDesc();
			if (trade != null) {
				trainingTradeRepository.delete(trade);
				System.out.println("Deleted last");
			}
		}
		//r
		if (keyCode == 82) {
			reset();
		}
		//b
		if (keyCode == 66) {
			breakEven();
		}
		enabled = false;	
	}

	private void breakEven() {
		if (candelStoploss != null) {
			Pageable page = PageRequest.of(0, 1000);
			List<Candel> candelsAfter = candelRepository.findByMarketAndTimeRangeAndDateAfterOrderByDateAsc(
					data.getMarket().getCode(),
					data.getTimeRange(),
					lastCandel.getDate(), 
					page);
			boolean breakEvenPossible = false;
			for (Candel candel : candelsAfter) {
				Double priceToBreakEven = priceOpen + spread;
				if (candel.getHigh() >= priceToBreakEven) {
					breakEvenPossible = true;
				}
				if (breakEvenPossible && candel.getLow() <= priceToBreakEven) {
					closeTrade(priceToBreakEven, candel.getDate());		
					return;
				}
				if (candel.getLow() <= candelStoploss.getLow()) {
					break;
				}
			}
			System.out.println("Could not break even");
		}
	}

	public void reset() {
		candelOpen = null;
		priceOpen = null;
		//		bestRiskRatio = 0d;
		candelStoploss = null;
		topPanel.setTradeInfo("0/3", Color.WHITE);
		topPanel.showDiff(0d, 0d);
//		data.getPricesToDraw().removeIf(HorizontalLine::isTypeStopLoss);
		data.getPricesToDraw().clear();
	}

	public void mouseMoved(LocalDateTime date, Double price) {
		if (candelStoploss != null && priceOpen != null) {
//			Double riskRatio = calculateRR(price);
//			topPanel.showDiff(riskRatio, 0d);
		}
	}

	public void addCandels(List<Candel> nextCandels) {
		if (candelStoploss != null) {
			for (Candel candel : nextCandels) {
				if (candel.getLow() < candelStoploss.getLow()) {
					closeTrade(candel.getLow(), candel.getDate());
					return;
				}
			}
		}
	}

	public void onLoadData(List<Candel> candels) {
		if (candels != null && !candels.isEmpty()) {
			lastCandel = Iterables.getLast(candels);
			if (candelStoploss != null) {
				Double riskRatio = calculateRR(lastCandel.getClose());
				topPanel.showDiff(riskRatio, 0d);
			}
		}
	}

}

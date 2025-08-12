package com.trading.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.trading.dto.MetaDeals;
import com.trading.dto.MetaPosition;
import com.trading.entity.Trade;
import com.trading.enums.EnumDirection;
import com.trading.enums.EnumTradeStatus;
import com.trading.metatrader.MetaBrokerApi;
import com.trading.repository.TradeRepository;
import com.trading.utils.DateHelper;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TradeService {

	@Autowired
	MetaBrokerApi metaBrokerApi;
	@Autowired
	TradeRepository tradeRepository;

	@Transactional
	public void fetchOpenTradesInfos() {
		List<MetaPosition> positions;
		try {
			positions = metaBrokerApi.getOpenPositions();
			positions.forEach(p -> {
				Optional<Trade> tradeOpt = tradeRepository.findById(p.getId());
				if (!tradeOpt.isPresent()) {
//					Trade t = Trade.builder()
//							.dateCreation(LocalDateTime.now())
//							.dateOpen(DateHelper.toLocalDateTime(p.getTime()))
//							.direction(p.getType().contains("BUY") ? EnumDirection.BUY : EnumDirection.SELL)
//							.identifier(p.getId())
//							.status(EnumTradeStatus.OPEN)
//							.volume(p.getVolume())
//							.market(p.getSymbol())
//							.stoploss(p.getStopLoss())
//							.priceOpen(p.getOpenPrice())
//							.build();
//					tradeRepository.save(t);
					log.info("New trade found but will not be saved to database");
				} else {
					Trade trade = tradeOpt.get();
					trade.setPriceOpen(p.getOpenPrice());
					trade.setStoploss(p.getStopLoss());
					trade.setStatus(EnumTradeStatus.OPEN);
					trade.setTakeProfit(p.getTakeProfit());
					tradeRepository.save(trade);
				}
			});
		} catch (IOException | InterruptedException e) {
			log.info("Error while fetching open trades");
		}

	}

	@Transactional
	public void checkTradeStatusFromBroker() {
		try {
			List<MetaDeals> deals = metaBrokerApi.getDealsHistory(LocalDateTime.now().minusDays(10), LocalDateTime.now().plusMinutes(1));
			deals.forEach(deal -> {
				Optional<Trade> t = tradeRepository.findById(deal.getPositionId());
				if (t.isPresent() 
						&& t.get().getStatus() == EnumTradeStatus.OPEN 
						&& deal.getEntryType().equals("DEAL_ENTRY_OUT")) {
					Trade trade = t.get();
					log.info("{} - Updating trade to status close", trade.getIdentifier());
					trade.setStatus(EnumTradeStatus.CLOSE);
					trade.setDateClose(DateHelper.toLocalDateTime(deal.getTime()));
					trade.setClosingReason(deal.getReason());
					trade.setPriceClose(deal.getPrice());
					trade.setProfitEuro(deal.getProfit());
//					trade.setStoploss(deal.getStopLoss());
//					trade.setTakeProfit(deal.getTakeProfit());
					double rr = (trade.getPriceClose() - trade.getPriceOpen())/(trade.getPriceOpen() - trade.getStoploss());
					trade.setRiskRatio(rr);
					tradeRepository.save(trade);
				}
			});
		} catch (IOException | InterruptedException e) {
			log.info("Error while fetching deals history");
		}
	}

	@Transactional
	public void openTrade(Trade trade) throws IOException {
		metaBrokerApi.openTrade(trade);
		//		trade.setIdentifier(trade.);
		if (trade.getIdentifier() != null) {
			tradeRepository.save(trade);
		} else {
			log.warn("Trade could not be saved because id id null");
		}
	}

	public void closeTrade(Trade trade) {
		try {
			metaBrokerApi.closeTrade(trade);
		} catch (IOException e) {
			log.error("Error closing trade", trade);
		} finally {
			checkTradeStatusFromBroker();
		}
	}

	public boolean modifyTrade(Trade trade) {
		try {
			boolean result = metaBrokerApi.modifyTrade(trade);
			return result;
		} catch (IOException e) {
			log.error("Error closing trade", trade);
			return false;
		} finally {
			fetchOpenTradesInfos();
		}
	}

}

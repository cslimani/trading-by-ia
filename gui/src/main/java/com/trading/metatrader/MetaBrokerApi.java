package com.trading.metatrader;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.MetaDeals;
import com.trading.dto.MetaPosition;
import com.trading.dto.MetaTrade;
import com.trading.entity.Trade;
import com.trading.enums.EnumDirection;
import com.trading.enums.EnumTradeStatus;
import com.trading.utils.DateHelper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
@Slf4j
public class MetaBrokerApi{

	@Autowired
	public ObjectMapper objectMapper;
	@Autowired
	public ServerManager serverManager;

	public OkHttpClient client = new OkHttpClient.Builder()
			.connectTimeout(10, TimeUnit.SECONDS)
			.writeTimeout(10, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS)
			.build();

	@Autowired
	MetaConfigProperties metaConfigProperties ;

	public boolean openTrade(Trade trade) throws IOException {
		String actionType = "ORDER_TYPE_BUY";
		if (trade.getDirection() == EnumDirection.SELL) {
			actionType = "ORDER_TYPE_SELL";	
		}
		MetaTradeRequest metaTradeRequest = MetaTradeRequest.builder()
				.actionType(actionType)
				.stopLoss(trade.getStoploss())
				.volume(trade.getVolume())
				.stopLossUnits("ABSOLUTE_PRICE")
				.symbol(trade.getMarket())
				.build();
		return tradeOperation(trade, metaTradeRequest);
	}
	
	public boolean tradeOperation(Trade trade, MetaTradeRequest metaTradeRequest) throws IOException {
	
		String json = objectMapper.writeValueAsString(metaTradeRequest);
		RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
		MetaServer server = serverManager.getServer();
		String url = server.getUrl() + "/trade";
		Request request = new Request.Builder()
				.addHeader("auth-token", metaConfigProperties.getToken())
				.url(url)
				.post(body)
				.build();
		log.info("Sending trade request operation {}", json);
		try {
			Response res = client.newCall(request).execute();
			String resBody = res.body().string();
			if (res.code() == 200) {
				if (resBody.contains("TRADE_RETCODE_DONE") || resBody.contains("ERR_NO_ERROR")) {
					MetaTrade metaTrade = objectMapper.readValue(resBody, MetaTrade.class);
					if (metaTrade.getTradeExecutionTime() != null) {
						trade.setDateOpen(DateHelper.toLocalDateTime(metaTrade.getTradeExecutionTime()));
					}
					if (metaTrade.getOrderId() != null) {
						trade.setIdentifier(metaTrade.getOrderId());
					}
					log.info("Success doing trade operation");
					return true;
				} else {
					log.error(url + " - ERROR " + resBody);
					log.error("Error creating trade - message {}", resBody);
					return false;
				}
			} else {
				log.error(url + " - ERROR " + resBody);
				log.error("Error creating trade code : {} - message {}", res.code(), resBody);
				return false;
			}
		} catch (Exception e) {
			server.error();
			log.error("Error creating trade", e);
			return false;
		} finally {
			server.release();
		}
	}
	
	private String repeatRequest(String url) throws IOException, InterruptedException {
		String resBody = null;
		int nbTry = 0;
		while ((resBody == null || resBody.contains("error")) && nbTry < 5) {
			MetaServer server = serverManager.getServer();
			try {
				resBody = executeRequest(server, url);
				if (resBody.contains("error")) {
					server.error();
					log.error("Error getting ticks with server {} - error {}", server, resBody);
					log.info(server.getUrl() + url + " - ERROR " + resBody);
					Thread.sleep(5000);
				} else {
					log.debug(server.getUrl() + url + " - OK");
				}
			} catch (Exception e) {
				server.error();
				log.error("Error doing request to metatrader API", e);
			} finally {
				server.release();
			}
			nbTry++;
		}
		if (nbTry >= 5) {
			log.error("Error doing request {} after 5 retry", url);
		}
		return resBody;
	}

	private String executeRequest(MetaServer server, String url) throws IOException {
		Request request = new Request.Builder()
				.url(server.getUrl() + url)
				.addHeader("auth-token", metaConfigProperties.getToken())
				.get()
				.build();
		int id = RandomUtils.nextInt(0, 1000000);
		log.debug("{} - Before request {}",id, server.getUrl() + url);
		Response res = client.newCall(request).execute();
		log.debug("{} - After request {}",id, server.getUrl() + url);
		return res.body().string();
	}

	public List<MetaPosition> getOpenPositions() throws IOException, InterruptedException {
		String url = "/positions";
		String resBody = repeatRequest(url);
		if (resBody.contains("error")) {
			return List.of();
		} else {
			return objectMapper.readValue(resBody, new TypeReference<List<MetaPosition>>(){});
		}
	}


	public List<MetaDeals> getDealsHistory(LocalDateTime dateStart, LocalDateTime dateEnd) throws IOException, InterruptedException {
		String url = String.format("/history-deals/time/%s/%s",DateHelper.toStringGMT(dateStart), DateHelper.toStringGMT(dateEnd));
		String resBody = repeatRequest(url);
		if (resBody.contains("error")) {
			return List.of();
		} else {
			return objectMapper.readValue(resBody, new TypeReference<List<MetaDeals>>(){});
		}
	}

	public void closeTrade(Trade trade) throws IOException {
		MetaTradeRequest metaTradeRequest = MetaTradeRequest.builder()
				.actionType("POSITION_CLOSE_ID")
				.positionId(trade.getIdentifier().toString())
				.build();
		tradeOperation(trade, metaTradeRequest);
	}

	public boolean modifyTrade(Trade trade) throws IOException {
		MetaTradeRequest metaTradeRequest = MetaTradeRequest.builder()
				.actionType("POSITION_MODIFY")
				.stopLoss(trade.getStoploss())
				.takeProfit(trade.getTakeProfit())
				.positionId(trade.getIdentifier().toString())
				.build();
		return tradeOperation(trade, metaTradeRequest);
	}

}

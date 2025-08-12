package com.trading.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.TickRecord;
import com.trading.entity.TickPrice;
import com.trading.utils.DateHelper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class PriceStreamingClient  {

	private ObjectMapper objectMapper = new ObjectMapper();

	private String streamingUrl = "http://127.0.0.1:8093";

	@Autowired
	PriceHolderService priceHolderService;

	public void connect() {
		log.info("Trying to connect to SSE Streaming on : " + streamingUrl);
		WebClient client = WebClient.create(streamingUrl);
		ParameterizedTypeReference<ServerSentEvent<String>> type = new ParameterizedTypeReference<ServerSentEvent<String>>() {
		};

		Flux<ServerSentEvent<String>> eventStream = client.get().uri("/stream-sse").retrieve().bodyToFlux(type);

		eventStream.subscribe(
				content -> consumeContent(content), 
				error -> processError(error),
				() -> subscriptionOver());

	}

	private void subscriptionOver() {
		log.info("SSE subscription completed, trying to reconnect...");
		connect();
	}

	private void processError(Throwable error) {
		log.error("Error receiving SSE", error);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			log.error("Error waiting before reconnection", e);
		}
		connect();
	}

	private void consumeContent(ServerSentEvent<String> content) {
		try {
			TickRecord tickRecord = objectMapper.readValue(content.data(), TickRecord.class);
			//						System.out.println(tickRecord.toString());
			LocalDateTime exactDate = DateHelper.toLocalDateTime(tickRecord.getTimestamp());
//			System.out.println("Price received for "  + tickRecord.getSymbol() + " - date " + exactDate);
			TickPrice tick = TickPrice.builder()
					.ask(tickRecord.getAsk())
					.bid(tickRecord.getBid())
					.date(DateHelper.toLocalDateTime(tickRecord.getTimestamp()))
					.market(tickRecord.getSymbol())
					.build();
			if (tick.getAsk() != null || tick.getBid() != null) {
				log.debug(tick.getMarket() + " - New price");
				priceHolderService.addTickToProcess(tick);
			}
		} catch (Exception e) {
			log.error("Error consuming SSE streaming", e);
		}
	}


}

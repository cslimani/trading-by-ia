package com.trading.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaDeals {

	private String symbol;
	private String reason;
	private String entryType;
	private Long positionId;
	private Double price;
	private Instant time;
	private Double profit;
	private Double stopLoss;
	private Double takeProfit;
}

package com.trading.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaPosition {

	Double openPrice;
	Double stopLoss;
	Double takeProfit;
	Long id;
	Instant time;
	Double volume;
	String type;
	String symbol;
}

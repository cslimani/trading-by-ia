package com.trading.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaTrade {

	private Long orderId;
	private Instant tradeExecutionTime;
}

package com.trading.dto;

import lombok.Data;

@Data
public class TickRecord  {

	private Double ask;
	private Double bid;
	private String symbol;
	private long timestamp;

}
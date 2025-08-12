package com.trading.metatrader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class MetaTradeRequest {

	String symbol;
	String actionType;
	String stopLossUnits;
	String takeProfitUnits;
	Double takeProfit;
	Double stopLoss;
	Double volume;
	String positionId;
}

package com.trading.dto;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor 
public class TakeProfit {

	@NonNull
	Double price;
	Boolean reached = false;
}

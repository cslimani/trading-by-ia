package com.trading.dto;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.trading.entity.Candel;
import com.trading.entity.Market;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class MarketDataDTO {


	private List<Candel> candels = new ArrayList<Candel>();
	private List<Candel> forwardingCandels = new ArrayList<Candel>();
	@NonNull
	private Market market;
	private Candel lastCandel;
	private ArrayList<LocalTime> tradingHoursList;

}

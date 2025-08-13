package com.trading.dto;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.trading.entity.Candle;
import com.trading.entity.Market;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class MarketDataDTO {


	private List<Candle> candles = new ArrayList<Candle>();
	private List<Candle> forwardingCandles = new ArrayList<Candle>();
	@NonNull
	private Market market;
	private Candle lastCandle;
	private ArrayList<LocalTime> tradingHoursList;

}

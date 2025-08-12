package com.trading.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.trading.dto.DataDTO;
import com.trading.feature.LiveTradingFeature;
import com.trading.repository.MarketRepository;

@Service
@Profile("live")
public class SchedulerService {

	@Autowired
	PriceService priceService;
	@Autowired
	PriceHolderService priceHolderService;
	@Autowired
	MarketRepository marketRepository;
	@Autowired
	DataDTO data;
	@Autowired
	LiveTradingFeature liveTradingFeature;


	@Scheduled(cron = "0 0/5 * * * *")
	public void synchroPricesCurrentMarket() throws IOException {
		priceHolderService.verifyCandels();
	}
	
	@Scheduled(cron = "0 * * * * *")
	public void checkAccumulations() throws IOException {
		liveTradingFeature.checkAccumulationsAndAlerts();
	}
}

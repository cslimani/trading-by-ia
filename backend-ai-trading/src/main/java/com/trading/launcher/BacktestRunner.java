package com.trading.launcher;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.trading.service.AbstractService;

import lombok.extern.slf4j.Slf4j;

@Component
@Profile("backtest")
@Slf4j
public class BacktestRunner extends AbstractService implements CommandLineRunner {
	
	
	@Override
	public void run(String... args) throws Exception {
		// TODO Auto-generated method stub
		
	}

}

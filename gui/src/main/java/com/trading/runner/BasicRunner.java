package com.trading.runner;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("basic")
@Component
public class BasicRunner extends AbstractRunner{

	@Override
	protected void runnerInit() {
		dateEnd = LocalDateTime.of(2025, 8, 1, 0, 0);
	}

}

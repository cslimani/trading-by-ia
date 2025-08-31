package com.trading.enums;

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EnumTimeRange {

	 H4(Duration.ofHours(4)),
     H1(Duration.ofHours(1)),
     M15(Duration.ofMinutes(15)),
     M5(Duration.ofMinutes(5)),
     M3(Duration.ofMinutes(3)),
     M2(Duration.ofMinutes(2)),
     M1(Duration.ofMinutes(1)),
     S30(Duration.ofSeconds(30));


	private final Duration duration;

	public Duration duration() {
		return duration;
	}

	public long bucketMillis() {
		return duration.toMillis();
	}

}

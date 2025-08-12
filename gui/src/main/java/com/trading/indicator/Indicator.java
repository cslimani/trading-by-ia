package com.trading.indicator;

import java.time.LocalDateTime;

public interface Indicator {

	Double get(LocalDateTime date);

}

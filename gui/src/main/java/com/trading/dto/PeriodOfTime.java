package com.trading.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PeriodOfTime {

	LocalDateTime dateStart;
	LocalDateTime dateEnd;
	
}

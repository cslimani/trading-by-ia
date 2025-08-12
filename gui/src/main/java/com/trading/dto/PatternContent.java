package com.trading.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.trading.enums.EnumTimeRange;

import lombok.Data;

@Data
public class PatternContent {

	LocalDateTime dateParentStart;
	LocalDateTime dateParentEnd;
	LocalDateTime dateParentMax;
	EnumTimeRange timerange;
	LocalDateTime dateEnterZone;
	Double priceEnterZone;
	Double reward;
	List<LocalDateTime> datesToColor;
	
	
}

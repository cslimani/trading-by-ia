package com.trading.dto;

import java.time.LocalDateTime;

import com.trading.enums.EnumTimeRange;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PointOfPattern {

	
	LocalDateTime date;
	EnumTimeRange timerange;
	
}

package com.trading.dto;

import java.time.LocalDateTime;

import com.trading.enums.EnumTimeRange;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Trade {

	LocalDateTime startDate;
	LocalDateTime endDate;
	Double riskRatio;
	Double tp;
	Double sl;
	EnumTimeRange timeRange;
	Double springUnderPrice;
	Double springAbovePrice;
	Integer nbBottoms;
	Double rangeMin;
	Double rangeMax;
	Double underRangeLimit;
	Double aboveRange1;
	Double aboveRange2;
	Double aboveRange3;
	Double height;
	
}

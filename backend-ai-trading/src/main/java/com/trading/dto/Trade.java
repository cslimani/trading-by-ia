package com.trading.dto;

import java.time.LocalDateTime;

import com.trading.enums.EnumTimeRange;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

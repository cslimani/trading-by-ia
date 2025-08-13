package com.trading.dto;

import java.time.LocalDateTime;

import com.trading.entity.Candle;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GraphDateDTO {

	Candle candle;
	LocalDateTime date;
	LocalDateTime endDate;
	int startPosition;
	int endPosition;
	
}

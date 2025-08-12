package com.trading.dto;

import java.time.LocalDateTime;

import com.trading.entity.Candel;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GraphDateDTO {

	Candel candel;
	LocalDateTime date;
	LocalDateTime endDate;
	int startPosition;
	int endPosition;
	
}

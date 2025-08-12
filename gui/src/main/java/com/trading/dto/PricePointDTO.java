package com.trading.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PricePointDTO {

	private Double price;
	private LocalDateTime date;
	
	
}

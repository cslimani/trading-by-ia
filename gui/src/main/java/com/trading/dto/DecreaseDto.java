package com.trading.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DecreaseDto {

	Double value;
	LocalDateTime date;
}

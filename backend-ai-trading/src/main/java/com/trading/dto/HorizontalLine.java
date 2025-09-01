package com.trading.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class HorizontalLine {

	Double price;
	String color;
	String type;
	LocalDateTime dateStart;
	
}

package com.trading.dto;

import java.awt.Color;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerticalLine {

	LocalDateTime date;
	Color color;
	
}

package com.trading.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Spring {

	Integer indexBreak;
	LocalDateTime dateBreak;
	boolean confirmed;
	Double confirmationPrice;
}

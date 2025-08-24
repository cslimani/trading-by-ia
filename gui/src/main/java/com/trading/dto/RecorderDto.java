package com.trading.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RecorderDto {

	private LocalDateTime datePeakBefore;
	private LocalDateTime dateSellingClimax;
	private LocalDateTime dateSpring;
	private LocalDateTime dateBreakAccumulation;
	
	public void nextDate(LocalDateTime date) {
		if (datePeakBefore == null) {
			datePeakBefore = date;
			return;
		}
		if (dateSellingClimax == null) {
			dateSellingClimax = date;
			return;
		}
		if (dateSpring == null) {
			dateSpring = date;
			return;
		}
		if (dateBreakAccumulation == null) {
			dateBreakAccumulation = date;
			return;
		}
	}
}

package com.trading.service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import com.trading.entity.Candle;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Range {

	@NonNull
	public LocalDateTime dateStart;
	@NonNull
	public LocalDateTime dateEnd;
	@NonNull
	public Integer indexStart;
	@NonNull
	public Integer indexEnd;
	@NonNull
	public Double high;
	@NonNull
	public Double low;
	@NonNull
	public Double height;
	@NonNull
	public Double max;
	@NonNull
	public Double min;
	
	public Double maxHeight;
	Candle swingHighBefore;
	Candle firstAccumulationCandle;
	AtomicInteger nbBreakDown = new AtomicInteger();
	AtomicInteger nbBreakUp = new AtomicInteger();
	Double rangeSwingHighRatio;
	
	public boolean isSame(Range newRange) {
		return newRange.getHeight() == height && newRange.getIndexStart() == indexStart;
	}
	
}

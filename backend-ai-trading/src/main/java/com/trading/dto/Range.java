package com.trading.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.trading.entity.Candle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Range {

	public LocalDateTime dateStart;
	public LocalDateTime dateEnd;
	public Integer indexStart;
	public Integer indexEnd;
	public Integer indexRangeValidated;
	public Integer width;
	public Double high;
	public Double low;
	public Double height;
	public Double max;
	public Double min;
	public Double minWithLow;
	public Double maxWithHigh;
	public Candle candleMin;
	public Candle candleMax;
	public LocalDateTime dateStartTrade;
	List<Candle> minList;
	List<Candle> maxList;
	List<Spring> springs;
	Trade trade;
	Integer sellingClimaxIndex;
	public Double maxHeight;
	Candle swingHighBefore;
	Candle firstAccumulationCandle;
	AtomicInteger nbBreakDown;
	AtomicInteger nbBreakUp;
	Double rangeSwingHighRatio;
	LocalDateTime dateDecisionRangeValid;
	Boolean breakFromTop;
	public boolean isSame(Range newRange) {
		return newRange.getHeight() == height && newRange.getIndexStart() == indexStart;
	}
	
	public int size() {
		return indexEnd - indexStart;
	}
	
}

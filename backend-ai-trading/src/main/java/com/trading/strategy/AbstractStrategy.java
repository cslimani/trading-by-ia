package com.trading.strategy;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.trading.entity.Candle;
import com.trading.enums.ExtremumType;
import com.trading.indicator.extremum.Extremum;

public class AbstractStrategy {

	public Candle getMinOfBottoms(List<Candle> minList) {
		return minList.stream()
				.filter(c -> !c.isRescued())
				.sorted(Comparator.comparingDouble(Candle::getMin)).findFirst().get();
	}


	public Candle getMaxOfTops(List<Candle> maxList) {
		return maxList.stream()
				.filter(c -> !c.isRescued())
				.sorted(Comparator.comparingDouble(Candle::getMax).reversed()).findFirst().get();
	}
	
	
	public Optional<Extremum> getFirstExtremumBefore(Candle c,  List<Extremum> extremums, ExtremumType type, boolean checkIndex) {
		return extremums.stream()
				.filter(e -> e.type == type)
				.filter(e -> {
					int shift = 0;
					if (checkIndex) {
						shift = e.getK();
					}
					return e.getCandle().getIndex() < c.getIndex() - shift;
				})
				.max(Comparator.comparingInt(e -> e.getCandle().getIndex()));
	}
}

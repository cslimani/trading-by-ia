package com.trading.indicator;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.trading.entity.Candel;

public class TryIndicator3 implements Indicator{

	Map<Integer, Double> map = new HashMap<>();
	Map<LocalDateTime, Double> mapDate = new HashMap<>();
	Map<Integer, Double> mapAverageUpwardMove = new HashMap<>();
	Map<Integer, Double> mapAverageDownwardMove = new HashMap<>();
	List<Candel> candels;
	private int nbPeriods;

	public TryIndicator3(List<Candel> candels, int nbPeriods) {
		this.candels = candels;
		this.nbPeriods = nbPeriods;
		init();
	}

	private void init() {
		Map<LocalDateTime, Double> mapDateTmp = new HashMap<>();
		for (int i = nbPeriods; i < candels.size(); i++) {
			Double value = candels.get(i-nbPeriods).getHigh() - candels.get(i).getHigh();
			if (value > 0) {
				mapDateTmp.put(candels.get(i).getDate(), value);
			}
		}

		Map<LocalDateTime, Double> mapAverage = 
				mapDateTmp.entrySet().stream().collect(Collectors.groupingBy(a -> a.getKey().truncatedTo(ChronoUnit.DAYS), Collectors.averagingDouble(e -> e.getValue())));
		mapDate = mapDateTmp.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> 	{
			return (e.getValue()/mapAverage.get(e.getKey().truncatedTo(ChronoUnit.DAYS)));
		}));

	}

	public Double get(Integer index) {
		return map.get(index);
	}

	public Double get(LocalDateTime date) {
		return mapDate.get(date);
	}

}

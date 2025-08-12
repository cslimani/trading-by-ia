package com.trading.indicator;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import com.trading.entity.Candel;

public class TryIndicator implements Indicator{

	Map<Integer, Double> map = new HashMap<>();
	Map<LocalDateTime, Double> mapDate = new HashMap<>();
	Map<Integer, Double> mapAverageUpwardMove = new HashMap<>();
	Map<Integer, Double> mapAverageDownwardMove = new HashMap<>();
	List<Candel> candels;
	private int nbPeriods;

	public TryIndicator(List<Candel> candels, int nbPeriods) {
		this.candels = candels;
		this.nbPeriods = nbPeriods;
		init();
	}

	private void init() {
		CircularFifoQueue<Double> queue = new CircularFifoQueue<>(nbPeriods);
		for (int i = 10*nbPeriods; i < candels.size(); i++) {
			queue.add(candels.get(i).getHigh() - candels.get(i).getLow());
			double sum = queue.stream().mapToDouble(v -> v).average().getAsDouble();
			map.put(i, sum);
			mapDate.put(candels.get(i).getDate(),sum);
//			System.out.println(sum);
		}
	}
	
	public Double get(Integer index) {
		return map.get(index);
	}
	
	public Double get(LocalDateTime date) {
		return mapDate.get(date);
	}

}

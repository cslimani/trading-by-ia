package com.trading.indicator;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import com.trading.entity.Candel;

public class TryIndicator2 implements Indicator{

	Map<Integer, Double> map = new HashMap<>();
	Map<LocalDateTime, Double> mapDate = new HashMap<>();
	Map<Integer, Double> mapAverageUpwardMove = new HashMap<>();
	Map<Integer, Double> mapAverageDownwardMove = new HashMap<>();
	List<Candel> candels;
	private int nbPeriods;

	public TryIndicator2(List<Candel> candels, int nbPeriods) {
		this.candels = candels;
		this.nbPeriods = nbPeriods;
		init();
	}

	private void init() {
		CircularFifoQueue<Integer> queue = new CircularFifoQueue<>(nbPeriods);
		for (int i = 1000; i < candels.size(); i++) {
			for (int j = 1; j < 1000; j++) {
				if (candels.get(i-j).getLow() <= candels.get(i).getLow()) {
					queue.add(j);
					break;
				}
				if (j == 999) {
					queue.add(j);
				}
			}
			double count = queue.stream().filter(v -> v > 100).count();
			map.put(i, count);
			mapDate.put(candels.get(i).getDate(), count);
		}
		System.out.println("TryIndicator2 finished init");
	}
	
	public Double get(Integer index) {
		return map.get(index);
	}
	
	public Double get(LocalDateTime date) {
		return mapDate.get(date);
	}

}

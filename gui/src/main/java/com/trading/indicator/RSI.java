package com.trading.indicator;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.trading.entity.Candel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RSI implements Indicator {

	Map<Integer, Double> map = new HashMap<>();
	Map<LocalDateTime, Double> mapDate = new HashMap<>();
	Map<Integer, Double> mapAverageUpwardMove = new HashMap<>();
	Map<Integer, Double> mapAverageDownwardMove = new HashMap<>();
	List<Candel> candels;
	private int nbPeriods;

	public RSI(List<Candel> candels, int nbPeriods) {
		this.candels = candels;
		this.nbPeriods = nbPeriods;
		init();
	}

	private void init() {
		Double sumFirstCandelsHigh = 0d;
		Double sumFirstCandelsLow = 0d;
		for (int i = 1; i <= nbPeriods; i++) {
			Candel candel = candels.get(i);
			Double diff = candel.getClose() - candels.get(i - 1).getClose();
			if (diff > 0) {
				sumFirstCandelsHigh += diff;
			} else {
				sumFirstCandelsLow += -diff;
			}
		}
		Double averageUpwardMove = sumFirstCandelsHigh / nbPeriods;
		Double averageDownwardMove = sumFirstCandelsLow / nbPeriods;
		Double RS = averageUpwardMove / averageDownwardMove;
		mapAverageUpwardMove.put(nbPeriods, averageUpwardMove);
		mapAverageDownwardMove.put(nbPeriods, averageDownwardMove);
		Double firstRSI = getRSI(RS);
		map.put(nbPeriods, firstRSI);
		for (int i = nbPeriods + 1; i < candels.size(); i++) {
			computeData(i);
		}
	}

	private void computeData(int index) {
		try {
			Double averageUpwardMove = mapAverageUpwardMove.get(index - 1);
			Double averageDownwardMove = mapAverageDownwardMove.get(index - 1);
			averageUpwardMove = (averageUpwardMove * (nbPeriods - 1) + getUpwardMove(index)) / nbPeriods;
			averageDownwardMove = (averageDownwardMove * (nbPeriods - 1) + getDownwardMove(index)) / nbPeriods;
			mapAverageUpwardMove.put(index, averageUpwardMove);
			mapAverageDownwardMove.put(index, averageDownwardMove);
			Double smoothedRS = averageUpwardMove / averageDownwardMove;
			Double rsi = getRSI(smoothedRS);
			map.put(index, rsi);
			mapDate.put(candels.get(index).getDate(), rsi);
		} catch (Exception e) {
			log.error("Error computing RSI", e);
			throw e;
		}
	}

	private Double getUpwardMove(int i) {
		Double move = candels.get(i).getClose() - candels.get(i - 1).getClose();
		return move > 0 ? move : 0;
	}

	private Double getDownwardMove(int i) {
		Double move = candels.get(i).getClose() - candels.get(i - 1).getClose();
		return move < 0 ? -move : 0;
	}

	private Double getRSI(Double rs) {
		return 100 - 100 / (1 + rs);
	}

	
	public Double get(Integer index) {
		return map.get(index);
	}
	
	public Double get(LocalDateTime date) {
		return mapDate.get(date);
	}

}

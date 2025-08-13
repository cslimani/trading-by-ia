package com.trading.indicator;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.trading.entity.Candle;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RSI implements Indicator {

	Map<Integer, Double> map = new HashMap<>();
	Map<LocalDateTime, Double> mapDate = new HashMap<>();
	Map<Integer, Double> mapAverageUpwardMove = new HashMap<>();
	Map<Integer, Double> mapAverageDownwardMove = new HashMap<>();
	List<Candle> candles;
	private int nbPeriods;

	public RSI(List<Candle> candles, int nbPeriods) {
		this.candles = candles;
		this.nbPeriods = nbPeriods;
		init();
	}

	private void init() {
		Double sumFirstCandlesHigh = 0d;
		Double sumFirstCandlesLow = 0d;
		for (int i = 1; i <= nbPeriods; i++) {
			Candle candle = candles.get(i);
			Double diff = candle.getClose() - candles.get(i - 1).getClose();
			if (diff > 0) {
				sumFirstCandlesHigh += diff;
			} else {
				sumFirstCandlesLow += -diff;
			}
		}
		Double averageUpwardMove = sumFirstCandlesHigh / nbPeriods;
		Double averageDownwardMove = sumFirstCandlesLow / nbPeriods;
		Double RS = averageUpwardMove / averageDownwardMove;
		mapAverageUpwardMove.put(nbPeriods, averageUpwardMove);
		mapAverageDownwardMove.put(nbPeriods, averageDownwardMove);
		Double firstRSI = getRSI(RS);
		map.put(nbPeriods, firstRSI);
		for (int i = nbPeriods + 1; i < candles.size(); i++) {
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
			mapDate.put(candles.get(index).getDate(), rsi);
		} catch (Exception e) {
			log.error("Error computing RSI", e);
			throw e;
		}
	}

	private Double getUpwardMove(int i) {
		Double move = candles.get(i).getClose() - candles.get(i - 1).getClose();
		return move > 0 ? move : 0;
	}

	private Double getDownwardMove(int i) {
		Double move = candles.get(i).getClose() - candles.get(i - 1).getClose();
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

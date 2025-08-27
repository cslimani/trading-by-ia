package com.trading.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.trading.entity.Candle;
import com.trading.entity.HotSpot;
import com.trading.enums.EnumTimeRange;
import com.trading.indicator.extremum.Extremum;
import com.trading.indicator.extremum.SwingExtremaFinder.Type;
import com.trading.repository.CandleRepository;
import com.trading.repository.HotSpotRepository;

public class AbstractService {

	@Autowired
	CandleRepository candleRepository;
	@Autowired
	HotSpotRepository hotSpotRepository;
	public Map<String, Integer> mapCount = new HashMap<String, Integer>();
	
	public void setIndex(List<Candle> candles) {
		for (int i = 0; i < candles.size(); i++) {
			candles.get(i).setIndex(i);
		}
	}
	
	public Integer getCount(String key) {
		return mapCount.get(key);
	}
	
	public Candle getFirstExtremumBefore(Candle c,  List<Extremum> extremums, Type type) {
		return extremums.stream()
				.filter(e -> e.type == type)
				.filter(e -> e.getDate().isBefore(c.getDate()))
				.max(Comparator.comparingInt(e -> e.getCandle().getIndex()))
				.get()
				.getCandle();
	}
	
	public void increaseCount(String key) {
		if (mapCount.containsKey(key)) {
			mapCount.put(key, mapCount.get(key) + 1);
		} else {
			mapCount.put(key, 1);
		}
	}
	
	public void displayMapCount() {
		System.out.println();
		mapCount.forEach((k,v) -> {
			System.out.println(k + " : " + v);
		});
	}
	
	public String format(LocalDateTime dateTime) {
		return dateTime.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
	}
	
	public void saveHotSpot(LocalDateTime dateStart, 
			LocalDateTime dateEnd,
			List<LocalDateTime> keyDates,
			String market, 
			EnumTimeRange timeRange, String code) {
		hotSpotRepository.save(HotSpot.builder()
				.market(market)
				.timeRange(timeRange)
				.dateStart(dateStart)
				.dateEnd(dateEnd)
				.keyDates(keyDates)
				.code(code)
				.creationDate(LocalDateTime.now())
				.build());
	}
}

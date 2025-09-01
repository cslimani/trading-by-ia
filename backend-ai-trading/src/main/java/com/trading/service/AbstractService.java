package com.trading.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.trading.dto.HotSpotData;
import com.trading.entity.Candle;
import com.trading.entity.HotSpot;
import com.trading.enums.EnumTimeRange;
import com.trading.indicator.extremum.Extremum;
import com.trading.indicator.extremum.SwingExtremaFinder.Type;
import com.trading.repository.CandleRepository;
import com.trading.repository.HotSpotRepository;

public class AbstractService {

	private static final Integer NB_MAX_PRICE = 50;
	public String sourcePath = "/data/trading_ml/bars_after_break.csv";
	public String targetFolder = "/data/trading_ml/backup";
	public List<Map<String, Object>> listMapFeatures = new CopyOnWriteArrayList<Map<String,Object>>();
	
	@Autowired
	public CandleRepository candleRepository;
	@Autowired
	public HotSpotRepository hotSpotRepository;
	public static Map<String, AtomicInteger> mapCountInteger = new ConcurrentHashMap<String, AtomicInteger>();
	public static Map<String, DoubleAdder> mapCountFloat = new ConcurrentHashMap<String, DoubleAdder>();
	
	public void setIndex(List<Candle> candles) {
		for (int i = 0; i < candles.size(); i++) {
			candles.get(i).setIndex(i);
		}
	}
	
	public Integer getCount(String key) {
		return mapCountInteger.get(key).get();
	}
	public Double getCountFloat(String key) {
		return mapCountFloat.get(key).doubleValue();
	}
	
	
	public void increaseCount(String key) {
		if (mapCountInteger.containsKey(key)) {
			mapCountInteger.get(key).incrementAndGet();
		} else {
			mapCountInteger.put(key, new AtomicInteger(1));
		}
	}
	
	public void increaseDouble(String key, Double value) {
		if (mapCountFloat.containsKey(key)) {
			 mapCountFloat.get(key).add(value);
		} else {
			DoubleAdder da = new DoubleAdder();
			mapCountFloat.put(key, da);
		}
	}
	
	public void displayMapCount() {
		System.out.println();
		mapCountInteger.forEach((k,v) -> {
			System.out.println(k + " : " + v);
		});
		System.out.println();
		mapCountFloat.forEach((k,v) -> {
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
			EnumTimeRange timeRange, String code,
			HotSpotData data) {
		hotSpotRepository.save(HotSpot.builder()
				.market(market)
				.timeRange(timeRange)
				.dateStart(dateStart)
				.dateEnd(dateEnd)
				.keyDates(keyDates)
				.code(code)
				.data(data)
				.creationDate(LocalDateTime.now())
				.build());
	}
	
	public Optional<Extremum> getFirstExtremumBefore(Candle c,  List<Extremum> extremums, Type type) {
		return extremums.stream()
				.filter(e -> e.type == type)
				.filter(e -> e.getCandle().getIndex() < c.getIndex() - e.getK())
				.max(Comparator.comparingInt(e -> e.getCandle().getIndex()));
	}

	public Optional<Extremum> getFirstExtremumBeforeAndLowerThan(Candle c,  List<Extremum> extremums, Type type, Double maxValue) {
		if (c == null) {
			return null;
		}
		return extremums.stream()
				.filter(e -> e.type == type)
				.filter(e -> e.getCandle().getIndex() < c.getIndex() - e.getK())
				.filter(e -> e.getPrice() < maxValue)
				.max(Comparator.comparingInt(e -> e.getCandle().getIndex()));
	}

	public void writeHeaders() throws IOException {
		if (listMapFeatures.isEmpty()) {
			return;
		}
		Map<String, Object> map = listMapFeatures.get(0);
		String header = map.keySet().stream().sorted().collect(Collectors.joining(","));
		writeLine(header);
	}

	public Candle getSpringCandle(List<Candle> candles, Range range, Candle candleBreak) {
		Candle accStart = range.getFirstAccumulationCandle();
		return candles.stream()
				.filter(c -> c.getIndex() > accStart.getIndex() && c.getIndex() < candleBreak.getIndex())
				.min(Comparator.comparingDouble(c -> c.getLow()))
				.get();
	}
	
	public void writeLine(String line) throws IOException {
		FileUtils.writeStringToFile(new File(sourcePath), line + System.lineSeparator(), Charset.defaultCharset(), true);
	}

	public void backupFile() {
		Utils.backupFile(sourcePath, targetFolder);
	}

	public Double normalize(double price, Range range) {
		return (price - range.getMin()) / range.getMaxHeight();
	}

	public boolean isInvalid(List<Double> list) {
		return list.stream()
				.anyMatch(d -> Double.isInfinite(d) || Double.isNaN(d));
	}
	
	public void addPriceFeature(Map<String, Object> mapFeature, List<Candle> candles, Integer currentIndex, Range range) {
		for(int i = 0 ; i < NB_MAX_PRICE; i++) {
			int tmpIndex = currentIndex - NB_MAX_PRICE + 1 + i;
			Candle c = candles.get(tmpIndex);
			int index = i +10;
			mapFeature.put("o_" + index, normalize(c.getOpen(), range));
			mapFeature.put("h_" + index, normalize(c.getHigh(), range));
			mapFeature.put("c_" + index, normalize(c.getClose(), range));
			mapFeature.put("l_" + index, normalize(c.getLow(), range));
			mapFeature.put("v_" + index, normalize(c.getVolume(), range));
		}
	}

	public boolean isTradeToTP(Candle cStart, List<Candle> candles, double tp, double sl) {
		for (int i = cStart.getIndex(); i < candles.size(); i++) {
			Candle c = candles.get(i);
			if (c.getHigh() >= tp) {
				return true;
			}
			if (c.getLow() <= sl) {
				return false;
			}
		}
		return false;
	}
	
	public void writeAll() throws IOException {
		writeHeaders();
		DecimalFormat df = new DecimalFormat("0.##"); 
		listMapFeatures.forEach(map -> {
			List<String> keys = map.keySet().stream().sorted().toList();
			String line = keys.stream()
					.map(k -> {
						Object value = map.get(k);
						if (value instanceof Double d) {
							return df.format(d);
						} else {
							return String.valueOf(value);
						}
					})
					.collect(Collectors.joining(","));
			try {
				writeLine(line);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}

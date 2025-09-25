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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.trading.dto.HotSpotData;
import com.trading.dto.Range;
import com.trading.dto.Trade;
import com.trading.entity.Candle;
import com.trading.entity.HotSpot;
import com.trading.enums.EnumTimeRange;
import com.trading.enums.ExtremumType;
import com.trading.indicator.extremum.Extremum;
import com.trading.repository.CandleRepository;
import com.trading.repository.HotSpotRepository;

public class AbstractService {

	protected static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static final Integer NB_MAX_PRICE = 20;
	public String sourcePath = "/data/trading_ml/classifier_";
	public String targetFolder = "/data/trading_ml/backup";
	public List<Map<String, Object>> listMapFeatures = new CopyOnWriteArrayList<Map<String,Object>>();
//	public boolean debug;
	
	@Autowired
	public CandleRepository candleRepository;
	@Autowired
	public HotSpotRepository hotSpotRepository;
	@Autowired
	private JdbcTemplate jdbcTemplate;
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

	public Optional<Candle> getIndexByDate(LocalDateTime date, List<Candle> candles) {
		return candles.stream()
				.filter(c -> c.getDate().isEqual(date))
				.findFirst();
	}

	public void displayMapCount() {
		System.out.println();
		mapCountInteger.entrySet().stream()
		.map(e -> e.getKey() + " : " + e.getValue())
		.sorted()
		.forEach(v -> System.out.println(v.contains("_") ? StringUtils.substringAfter(v, "_") : v));

		mapCountFloat.entrySet().stream()
		.map(e -> e.getKey() + " : " + e.getValue())
		.sorted()
		.forEach(v -> System.out.println(v.contains("_") ? StringUtils.substringAfter(v, "_") : v));
	}

	public void setSourceFileName(String suffix) {
		this.sourcePath =  this.sourcePath + suffix + ".csv";		
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

	public Optional<Extremum> getFirstExtremumBeforeAndLowerThan(Candle c,  List<Extremum> extremums, ExtremumType type, Double value, boolean lower) {
		if (c == null) {
			return null;
		}
		return extremums.stream()
				.filter(e -> e.type == type)
				.filter(e -> e.getCandle().getIndex() < c.getIndex() - e.getK())
				.filter(e -> {
					return 	lower ? e.getPrice() < value : e.getPrice() > value;
				})
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
		return (price - range.getMin()) / range.getHeight();
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

	public void addPriceFeature(Map<String, Object> mapFeature, List<Candle> candles, Integer startIndex, Integer endIndex, Trade trade) {
		int index = 0;
		for(int i = startIndex ; i <= Math.min(endIndex, startIndex + NB_MAX_PRICE - 1); i++) {
			Candle c = candles.get(i);
			mapFeature.put("o_" + index, normalize(c.getOpen(), trade));
//			mapFeature.put("h_" + index, normalize(c.getHigh(), range));
			mapFeature.put("c_" + index, normalize(c.getClose(), trade));
//			mapFeature.put("l_" + index, normalize(c.getLow(), range));
//			mapFeature.put("v_" + index, normalize(c.getVolume(), range));
			index++;
		}
		while (index < NB_MAX_PRICE) {
			mapFeature.put("o_" + index, Double.NaN);
			mapFeature.put("c_" + index, Double.NaN);
			index++;
		}
//		System.out.println(mapFeature.size());
	}
	
	public Double normalize(double price, Trade trade) {
		return (price - trade.getRangeMin()) / trade.getHeight();
	}

	public boolean isTradeToTPBuy(Candle cStart, List<Candle> candles, double tp, double sl) {
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

	public boolean isTradeToTPSell(Candle cStart, List<Candle> candles, double tp, double sl) {
		for (int i = cStart.getIndex(); i < candles.size(); i++) {
			Candle c = candles.get(i);
			if (c.getLow() <= tp) {
				return true;
			}
			if (c.getHigh() >= sl) {
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
	
	public double getRangeSwingHighRatio(Range range) {
		Double priceSwingHigh = range.getSwingHighBefore().getHigh();
		Double priceRangeTop = range.getMax();
		Double priceRangeBottom = range.getMin();
		Double rangeHeight =  priceRangeTop - priceRangeBottom;
		range.setMaxHeight(rangeHeight);
		return rangeHeight / (priceSwingHigh - priceRangeBottom);
	}
	
	 public int backupHotSpots() {
	        String sql = """
	            INSERT INTO hot_spot_backup (
	        			creation_date,	 date_end,	 date_start,	 market,	 time_range,
	        		 code,	 key_dates,	 data,	 backup_date	            )
	         SELECT	  creation_date,	 date_end,	 date_start,	 market,
	        		 time_range,	 code,	 key_dates,	 data,	 ?
	            FROM hot_spot
	            """;
	        LocalDateTime backupDate = LocalDateTime.now();
	        return jdbcTemplate.update(sql, backupDate);
	    }

}

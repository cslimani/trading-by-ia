package com.trading.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import com.trading.entity.Candle;
import com.trading.enums.EnumTimeRange;
import com.trading.indicator.extremum.Extremum;
import com.trading.indicator.extremum.SwingExtremaFinder.Type;

@Component
public class FeatureWriter extends AbstractService{

	private String sourcePath = "/data/trading_ml/bars_after_break.csv";
	private String targetFolder = "/data/trading_ml/backup";
	List<Map<String, Double>> listMapFeatures = new ArrayList<>();

	public Integer addFeature(List<Candle> candles, Range range, Candle breakCandle, List<Extremum> extremums2,
			String market, EnumTimeRange timeRange, String hotspotCode) throws IOException {
		Integer indexEnd = breakCandle.getIndex();
		for(int i = breakCandle.getIndex() + 1; i < candles.size(); i++) {
			Candle c = candles.get(i);
//			if (c.isDate(2, 1) && c.isTime(20, 36, 0)) {
//				System.out.println();
//			}
			indexEnd = i;
			double tp = range.getMax() + range.getHeight();
			Candle slCandle = getFirstExtremumBeforeAndLowerThan(c, extremums2, Type.MIN, c.getOpen());
			double sl = slCandle.getLow();
			Candle swingHighBefore = range.getSwingHighBefore();
			if (c.getHigh() > tp) {
				break;
			}
			if (c.getLow() < range.getMin()) {
				break;
			}
			if (i > breakCandle.getIndex() + 100) {
				break;
			}
			
			double startTradePrice = c.getOpen();
			double rr = (tp - startTradePrice) / (startTradePrice - sl);
			boolean isTP = isTradeToTP(c, candles, tp, sl);
			if (isTP) {
				increaseCount("TP");
			} else {
				increaseCount("SL");
			}
				
			Map<String, Double> mapFeature = new HashMap<String, Double>();
			mapFeature.put("timestamp", Utils.getTimestamp(c.getDate()));
			mapFeature.put("accum_id", Utils.getTimestamp(swingHighBefore.getDate()));
			mapFeature.put("t_since_break", Double.valueOf(i - breakCandle.getIndex()));
			mapFeature.put("risk_ratio", rr);
			mapFeature.put("range_swing_high_ratio", range.getRangeSwingHighRatio());
			mapFeature.put("accum_size", Double.valueOf(breakCandle.getIndex() - range.getIndexStart()));
			mapFeature.put("y", isTP ? 1d : 0d);
			listMapFeatures.add(mapFeature);
			
			List<LocalDateTime> keyDates = List.of(swingHighBefore.getDate(), breakCandle.getDate(),c.getDate(), slCandle.getDate());
//			saveHotSpot(swingHighBefore.getDate(), c.getDate(), keyDates, market, timeRange, "RANGE_AUTO");
		}
		System.out.println("New accumulation " + breakCandle.getDate());
		return indexEnd;
	}

	private boolean isTradeToTP(Candle cStart, List<Candle> candles, double tp, double sl) {
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

	public Candle getFirstExtremumBefore(Candle c,  List<Extremum> extremums, Type type) {
		return extremums.stream()
				.filter(e -> e.type == type)
				.filter(e -> e.getCandle().getIndex() < c.getIndex() - e.getK())
				.max(Comparator.comparingInt(e -> e.getCandle().getIndex()))
				.get()
				.getCandle();
	}
	
	public Candle getFirstExtremumBeforeAndLowerThan(Candle c,  List<Extremum> extremums, Type type, Double maxValue) {
		return extremums.stream()
				.filter(e -> e.type == type)
				.filter(e -> e.getCandle().getIndex() < c.getIndex() - e.getK())
				.filter(e -> e.getPrice() < maxValue)
				.max(Comparator.comparingInt(e -> e.getCandle().getIndex()))
				.get()
				.getCandle();
	}

	public void writeHeaders() throws IOException {
		if (listMapFeatures.isEmpty()) {
			return;
		}
		Map<String, Double> map = listMapFeatures.get(0);
		String header = map.keySet().stream().sorted().collect(Collectors.joining(","));
		writeLine(header);
	}

	private void writeLine(String line) throws IOException {
		FileUtils.writeStringToFile(new File(sourcePath), line + System.lineSeparator(), Charset.defaultCharset(), true);
	}

	public void backupFile() {
		Utils.backupFile(sourcePath, targetFolder);
	}

	public void writeAll() throws IOException {
		writeHeaders();
		DecimalFormat df = new DecimalFormat("0.#####"); 
		listMapFeatures.forEach(map -> {
			List<String> keys = map.keySet().stream().sorted().toList();
			String line = keys.stream()
					.map(k -> df.format( map.get(k)))
					.collect(Collectors.joining(","));
			try {
				writeLine(line);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

}

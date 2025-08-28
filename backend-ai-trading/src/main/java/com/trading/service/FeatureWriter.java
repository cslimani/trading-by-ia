package com.trading.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.trading.entity.Candle;
import com.trading.indicator.extremum.Extremum;

public class FeatureWriter {

	private String sourcePath = "/data/trading_ml/bars_after_break.csv";
	private String targetFolder = "/data/trading_ml/backup";
	List<Map<String, Double>> listMapFeatures = new ArrayList<>();

	public void addFeature(List<Candle> candles, Range range, Candle c, List<Extremum> extremums2) throws IOException {
		Map<String, Double> mapFeature = new HashMap<String, Double>();
		
		mapFeature.put("timestamp", Utils.getTimestamp(c.getDate()));
		mapFeature.put("accum_id", Utils.getTimestamp(range.getSwingHighBefore().getDate()));
		mapFeature.put("y", Double.valueOf(new Random().nextInt(0, 2)));
		mapFeature.put("t_since_break", 1d);
		mapFeature.put("accum_size", Double.valueOf(range.getIndexEnd() - range.getIndexStart()));
		listMapFeatures.add(mapFeature);
		System.out.println("New accumulation " + c.getDate());
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

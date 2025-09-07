package com.trading.tech;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.trading.dto.Tick;
import com.trading.entity.Candle;
import com.trading.enums.EnumTimeRange;
import com.trading.repository.CandleRepository;

@Component
public class TickToCandles {

	@Autowired
	CandleRepository candleRepository;
	@Autowired
	JdbcTemplate jdbcTemplate;

	public enum PriceSource { MID, ASK, BID }
	ZoneId zoneUTC = ZoneId.of("UTC");
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss:SSS");

	public Map<EnumTimeRange, List<Candle>> buildByTicks(String market, List<Tick> ticks) {
		return build(market, ticks,
				EnumSet.of(
						EnumTimeRange.M30, EnumTimeRange.M10,EnumTimeRange.M3),
//				EnumSet.of(
//						EnumTimeRange.H4	,EnumTimeRange.H1, EnumTimeRange.M15,
//						EnumTimeRange.M30, EnumTimeRange.M10,EnumTimeRange.M3,
//						EnumTimeRange.M5, EnumTimeRange.M2,  EnumTimeRange.M1, EnumTimeRange.S30),
				PriceSource.BID);
	}

//	public Map<EnumTimeRange, List<Candle>> buildByLines(String market, List<String> csvLines) {
//		List<Tick> ticks = parseTicks(csvLines);
//		return build(market, ticks,
//				EnumSet.of(EnumTimeRange.H4	,EnumTimeRange.H1, EnumTimeRange.M15,EnumTimeRange.M30, EnumTimeRange.M10,
//						EnumTimeRange.M5, EnumTimeRange.M2, EnumTimeRange.M3, EnumTimeRange.M1, EnumTimeRange.S30),
//				PriceSource.BID);
//	}

	public Map<EnumTimeRange, List<Candle>> build(String market, List<Tick> ticks,
			Set<EnumTimeRange> timeframes,
			PriceSource priceSource) {
		ticks.sort(Comparator.comparing(t -> t.time));

		Map<EnumTimeRange, NavigableMap<Long, Agg>> perTfAgg = new EnumMap<>(EnumTimeRange.class);
		for (EnumTimeRange tf : timeframes) perTfAgg.put(tf, new TreeMap<>());

		for (Tick tick : ticks) {
			for (EnumTimeRange tf : timeframes) {
				long bucketMs = tf.bucketMillis();
				long tMs = tick.time.toEpochMilli();
				long bucketStartMs = (tMs / bucketMs) * bucketMs;
				NavigableMap<Long, Agg> aggMap = perTfAgg.get(tf);
				Agg agg = aggMap.get(bucketStartMs);
				double p = tick.price(priceSource);
				if (agg == null) {
					agg = new Agg(p);
					aggMap.put(bucketStartMs, agg);
				} else {
					agg.update(p);
				}
			}
		}

		Map<EnumTimeRange, List<Candle>> out = new EnumMap<>(EnumTimeRange.class);
		List<Candle> candles = new ArrayList<Candle>(1000_000);
		for (EnumTimeRange tf : timeframes) {
			for (Map.Entry<Long, Agg> e : perTfAgg.get(tf).entrySet()) {
				long startMs = e.getKey();
				Agg a = e.getValue();
				Candle c = new Candle(
						market,
						tf,
						Instant.ofEpochMilli(startMs),
						a.open, a.high, a.low, a.close, a.volume
						);
				candles.add(c);
			}
		}

		saveCandles(candles);
		return out;
	}

	private void saveCandles(List<Candle> candles) {
		//      candles.stream().forEach(c -> System.out.println(c.getDate() + " " + c.getTimeRange()));
//		System.out.println("saveCandles " + candles.size());
//		Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		jdbcTemplate.batchUpdate(
			    "INSERT INTO candle (date, open, close, high, low, market, time_range, volume) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
			    candles,
			    1000,
			    (ps, c) -> {
			    	ps.setTimestamp(1, Timestamp.from(c.getDateInstant()));
			        ps.setDouble(2, c.getOpen());
			        ps.setDouble(3, c.getClose());
			        ps.setDouble(4, c.getHigh());
			        ps.setDouble(5, c.getLow());
			        ps.setString(6, c.getMarket());
			        ps.setString(7, c.getTimeRange().toString());
			        ps.setDouble(8, c.getVolume());
			    }
			);
	}

	/** Petit accumulateur interne pour une bougie. */
	private final class Agg {
		double open, high, low, close;
		long volume;

		Agg(double first) {
			open = high = low = close = first;
			volume = 1;
		}

		void update(double price) {
			if (price > high) high = price;
			if (price < low)  low = price;
			close = price;
			volume++;
		}
	}

	// -------------------- Parsing --------------------

	public List<Tick> parseTicks(List<String> lines) {
		List<Tick> out = new ArrayList<>(lines.size());
		for (String raw : lines) {
			Tick tick = parseTick(raw);
			if (tick != null) {
				out.add(tick);
			}
		}
		return out;
	}


	public Tick parseTick(String raw) {
		if (raw == null) return null;
		//         String line = raw.trim();
		//         if (line.isEmpty()) return null;
		// Ignorer entêtes ou lignes non conformes
		//         if (line.startsWith("DATE") || line.startsWith("#") || line.equals("...")) return null;

		// Supporte les séparateurs virgule OU point-virgule
		String[] parts = raw.split(",");
		//         if (parts.length < 3) return null;

		String ts = parts[0].trim();
		String askStr = parts[1].trim();
		String bidStr = parts[2].trim();

		try {
			Instant t = parseTimestamp(ts);
			double ask = Double.parseDouble(askStr);
			double bid = Double.parseDouble(bidStr);
			return new Tick(t, ask, bid);
		} catch (Exception ignored) {
			return null;
		}
	}

	private Instant parseTimestamp(String s) {
		return LocalDateTime.parse(s, DATE_TIME_FORMATTER).atZone(zoneUTC).toInstant();
	}

	

	//    public void main(String[] args) {
	//        List<String> ticks = Arrays.asList(
	//                "DATE;ASK;BID",
	//                "20240906 11:26:34:930,18706.808,18710.282",
	//                "20240906 11:26:35:100,18707.100,18710.600",
	//                "20240906 11:27:00:005,18708.000,18711.500",
	//                "..."
	//        );
	//
	//        Map<EnumTimeRange, List<Candle>> result = TickToCandles.buildAll(ticks);
	//
	//        // Affiche les premières bougies M1 par exemple
	//        List<Candle> m1 = result.get(EnumTimeRange.M1);
	//        for (int i = 0; i < Math.min(3, m1.size()); i++) {
	//            System.out.println(m1.get(i));
	//        }
	//    }
}


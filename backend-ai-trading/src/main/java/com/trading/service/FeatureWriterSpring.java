package com.trading.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.trading.entity.Candle;
import com.trading.enums.EnumTimeRange;
import com.trading.indicator.extremum.Extremum;
import com.trading.indicator.extremum.SwingExtremaFinder.Type;

@Component
public class FeatureWriterSpring extends AbstractService{


	public Integer addFeature(List<Candle> candles, Range range, Candle breakCandle, List<Extremum> extremumsEntry,
			String market, EnumTimeRange timeRange, String hotspotCode) throws IOException {
		Integer indexEnd = breakCandle.getIndex();
		Candle swingHighBefore = range.getSwingHighBefore();
		Candle candleStartTrade = candles.get(indexEnd);
		double startTradePrice = candleStartTrade.getOpen();
		Candle previousCandle = candles.get(indexEnd-1);
		double distanceOpenSlAtr = 0;
		double distanceOpenSl = 0;
		int countSL = 0;

		Candle slCandle = previousCandle;
		while (distanceOpenSlAtr < 1 && countSL++ < 5) {
			Optional<Extremum> extremumOpt = getFirstExtremumBeforeAndLowerThan(slCandle, extremumsEntry, Type.MIN, startTradePrice);
			if (extremumOpt.isPresent()) {
				slCandle = extremumOpt.get().getCandle();
				distanceOpenSl = startTradePrice - slCandle.getLow();
				distanceOpenSlAtr = distanceOpenSl/previousCandle.getAtr();
			} else {
				continue;
			}
		}
		if (countSL >=5 || slCandle.getIndex().equals(previousCandle.getIndex())) {
//			increaseCount("FEATURE NO SL FOUND");
			return 0;
		}
		//			double tp = startTradePrice + distanceOpenSl;
		double tp = swingHighBefore.getLow();
		double sl = slCandle.getLow();
		double rr = (tp - startTradePrice) / (startTradePrice - sl);

		if (rr < 1) {
			increaseCount("FEATURE TP < 1");
			return 0;
		}

		boolean isTP = isTradeToTP(candleStartTrade, candles, tp, sl);
		if (isTP) {
			increaseDouble("RR", rr);
			increaseCount("FEATURE TP");
		} else {
			increaseDouble("RR", -1d);
			increaseCount("FEATURE SL");
		}
		Candle springCandle = getSpringCandle(candles, range, candleStartTrade);

		Map<String, Object> mapFeature = new HashMap<String, Object>();
		mapFeature.put("timestamp", Utils.getTimestamp(candleStartTrade.getDate()));
		mapFeature.put("accum_id", market + "_" + Utils.getTimestamp(swingHighBefore.getDate()));
		mapFeature.put("f_spring_index", springCandle.getIndex() - range.getIndexStart());
		mapFeature.put("f_accum_size", Double.valueOf(breakCandle.getIndex() - range.getIndexStart()));
		mapFeature.put("f_time_since_break", 0);
		mapFeature.put("date", breakCandle.getDate());
		mapFeature.put("y", isTP ? 1d : 0d);
		//				mapFeature.put("y", new Random().nextBoolean() ? 1d : 0d);
		//			addPriceFeature(mapFeature, candles, candleStartTrade.getIndex(), range);
		listMapFeatures.add(mapFeature);
		//			System.out.println("RR " + rr + " y=" + isTP);
		//			System.out.println("New accumulation " + breakCandle.getDate() + "SL at : " + slCandle.getDate());
		List<LocalDateTime> keyDates = List.of(swingHighBefore.getDate(),
				breakCandle.getDate(),
				candleStartTrade.getDate(),
				slCandle.getDate(),
				springCandle.getDate());
		saveHotSpot(swingHighBefore.getDate(), candleStartTrade.getDate(), keyDates, market, timeRange, "RANGE_AUTO");
		return 0;
	}

}

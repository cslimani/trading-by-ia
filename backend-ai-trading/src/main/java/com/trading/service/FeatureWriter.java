package com.trading.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.trading.dto.TakeProfit;
import com.trading.entity.Candle;
import com.trading.enums.EnumTimeRange;
import com.trading.indicator.extremum.Extremum;
import com.trading.indicator.extremum.SwingExtremaFinder.Type;

@Component
public class FeatureWriter extends AbstractService{



	public Integer addFeature(List<Candle> candles, Range range, Candle breakCandle, List<Extremum> extremums3,
			String market, EnumTimeRange timeRange, String hotspotCode) throws IOException {
		Integer indexEnd = breakCandle.getIndex();
		Candle swingHighBefore = range.getSwingHighBefore();
		double tp1 = range.getMax() + range.getHeight();
		double tp2 = range.getMin() + (swingHighBefore.getClose() - range.getMin())/2;
		double tp3 = range.getSwingHighBefore().getClose();
		List<TakeProfit> tpList = List.of(
				new TakeProfit(tp1)
//				new TakeProfit(tp2),
//				new TakeProfit(tp3)
		);
		for(int i = breakCandle.getIndex() + 1; i < candles.size(); i++) {
			Candle c = candles.get(i);
			
			//			if (c.isDate(2, 1) && c.isTime(20, 36, 0)) {
			//				System.out.println();
			//			}
			indexEnd = i;
			
			if (tpList.stream().filter(TakeProfit::getReached).count() == tpList.size()) {
				return c.getIndex();
			}
			if (c.getLow() < range.getMin()) {
				return c.getIndex();
			}
			if (i > breakCandle.getIndex() + 50) {
				return -1;
			}
			
			for (TakeProfit tp : tpList) {
				if (tp.getReached()) {
					continue;
				}
				double distanceOpenSL = 0;
				int countSL = 0;
				Candle slCandle = c;
				double startTradePrice = c.getOpen();
				while (distanceOpenSL < 2 && countSL++ < 5) {
//					slCandle = getFirstExtremumBeforeAndLowerThan(slCandle, extremums3, Type.MIN, c.getOpen());
					distanceOpenSL = (c.getOpen() - slCandle.getLow())/c.getAtr();
				}
				double sl = slCandle.getLow();
				double rr = (tp.getPrice() - startTradePrice) / (startTradePrice - sl);
				
				if (distanceOpenSL < 2) {
					increaseCount("SL_TOO_SMALL");
					continue;
				}
				if (c.getHigh() > tp.getPrice()) {
					tp.setReached(true);
					continue;
				}
				if (rr < 1) {
					increaseCount("TP < 1");
					continue;
				}

				boolean isTP = isTradeToTP(c, candles, tp.getPrice(), sl);
				if (isTP) {
					increaseCount("TP");
				} else {
					increaseCount("SL");
				}

				double swingHighDistance = (swingHighBefore.getClose() - c.getClose())/range.getMaxHeight();
				
				Map<String, Object> mapFeature = new HashMap<String, Object>();
				mapFeature.put("timestamp", Utils.getTimestamp(c.getDate()));
				mapFeature.put("accum_id", Utils.getTimestamp(swingHighBefore.getDate()));
				mapFeature.put("t_since_break", Double.valueOf(i - breakCandle.getIndex()));
				mapFeature.put("risk_ratio", rr);
				mapFeature.put("range_swing_high_ratio", range.getRangeSwingHighRatio());
				mapFeature.put("swing_high_distance", swingHighDistance);
				mapFeature.put("accum_size", Double.valueOf(breakCandle.getIndex() - range.getIndexStart()));
				mapFeature.put("y", isTP ? 1d : 0d);
//				mapFeature.put("y", new Random().nextBoolean() ? 1d : 0d);
				addPriceFeature(mapFeature, candles, c.getIndex(), range);
//				listMapFeatures.add(mapFeature);
				System.out.println("New accumulation " + breakCandle.getDate());
				List<LocalDateTime> keyDates = List.of(swingHighBefore.getDate(), breakCandle.getDate(),c.getDate(), slCandle.getDate());
			};
			//			saveHotSpot(swingHighBefore.getDate(), c.getDate(), keyDates, market, timeRange, "RANGE_AUTO");
		}
		
		return indexEnd;
	}


	

}

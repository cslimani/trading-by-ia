package com.trading.service.old;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.trading.dto.Range;
import com.trading.entity.Candle;
import com.trading.enums.EnumTimeRange;
import com.trading.enums.ExtremumType;
import com.trading.indicator.extremum.Extremum;
import com.trading.service.AbstractService;
import com.trading.service.Utils;

@Component
public class FeatureWriter2 extends AbstractService{

	private static final Integer NB_MAX_PRICE = 40;


	public Integer addFeature(List<Candle> candles, Range range, Candle breakCandle, List<Extremum> extremumsEntry,
			String market, EnumTimeRange timeRange, String hotspotCode) throws IOException {
		Integer indexEnd = breakCandle.getIndex();
		Candle swingHighBefore = range.getSwingHighBefore();
		double nbCandlesAboveRange = 0;
		double nbCandlesUnderRange = 0;
		for(int i = breakCandle.getIndex() + 1; i < candles.size(); i++) {
			Candle candleStartTrade = candles.get(i);
			double startTradePrice = candleStartTrade.getOpen();
			Candle previousCandle = candles.get(i-1);

			//			if (c.isDate(2, 1) && c.isTime(20, 36, 0)) {
			//				System.out.println();
			//			}
			indexEnd = i;

			if (candleStartTrade.getLow() < range.getMin()) {
				return candleStartTrade.getIndex();
			}
			if (i > breakCandle.getIndex() + 20) {
				return -1;
			}
			if (candleStartTrade.getHigh() > swingHighBefore.getLow()) {
				return candleStartTrade.getIndex();
			}
			if (previousCandle.getClose() > range.getMax()) {
				nbCandlesAboveRange += 1;
			}
			if (previousCandle.getClose() < range.getMax()) {
				nbCandlesUnderRange++;
			}
			if (nbCandlesAboveRange < 4 || nbCandlesUnderRange < 4 || previousCandle.getClose() < breakCandle.getHigh()) {
				continue;
			}
			double distanceOpenSlAtr = 0;
			double distanceOpenSl = 0;
			int countSL = 0;
			
			double breakStrength = getCandleStrength(breakCandle);
			double currentStrength = getCandleStrength(previousCandle);
			double distanceToRangeATR = (previousCandle.getOpen() - range.getMax())/ previousCandle.getAtr();
			double swingHighDistanceATR = (swingHighBefore.getClose() - previousCandle.getClose()) / breakCandle.getAtr();
				
			Candle slCandle = previousCandle;
			while (distanceOpenSlAtr < 1 && countSL++ < 5) {
				Optional<Extremum> extremumOpt = getFirstExtremumBeforeAndLowerThan(slCandle, extremumsEntry, ExtremumType.MIN, startTradePrice, true);
				if (extremumOpt.isPresent()) {
					slCandle = extremumOpt.get().getCandle();
					distanceOpenSl = startTradePrice - slCandle.getLow();
					distanceOpenSlAtr = distanceOpenSl/previousCandle.getAtr();
				} else {
					continue;
				}
			}
			if (countSL >=5 || slCandle.getIndex().equals(previousCandle.getIndex())) {
				break;
			}
//			double tp = startTradePrice + distanceOpenSl;
			double tp = swingHighBefore.getHigh();
			double sl = slCandle.getLow();
			double rr = (tp - startTradePrice) / (startTradePrice - sl);
			
			if (rr < 1) {
				increaseCount("TP < 1");
				continue;
			}
			
			boolean isTP = isTradeToTPBuy(candleStartTrade, candles, tp, sl);
			if (isTP) {
				increaseCount("TP");
			} else {
				increaseCount("SL");
			}

			Map<String, Object> mapFeature = new HashMap<String, Object>();
			mapFeature.put("timestamp", Utils.getTimestamp(candleStartTrade.getDate()));
			mapFeature.put("accum_id", market + "_" + Utils.getTimestamp(swingHighBefore.getDate()));
			mapFeature.put("f_time_since_break", Double.valueOf(i - breakCandle.getIndex()));
//			mapFeature.put("risk_ratio", rr);
			mapFeature.put("f_range_swing_high_ratio", range.getRangeSwingHighRatio());
			mapFeature.put("f_breakStrength", breakStrength);
			mapFeature.put("f_currentStrength", currentStrength);
			mapFeature.put("f_distanceToRangeATR", distanceToRangeATR);
			mapFeature.put("f_swing_high_distance", swingHighDistanceATR);
			mapFeature.put("f_nb_candles_above_range", nbCandlesAboveRange);
			mapFeature.put("f_nb_candles_under_range", nbCandlesUnderRange);
			mapFeature.put("f_sl_distance", distanceOpenSlAtr);
			mapFeature.put("f_accum_size", Double.valueOf(breakCandle.getIndex() - range.getIndexStart()));
			mapFeature.put("date", breakCandle.getDate());
			mapFeature.put("y", isTP ? 1d : 0d);
			//				mapFeature.put("y", new Random().nextBoolean() ? 1d : 0d);
//			addPriceFeature(mapFeature, candles, candleStartTrade.getIndex(), range);
			listMapFeatures.add(mapFeature);
//			System.out.println("RR " + rr + " y=" + isTP);
//			System.out.println("New accumulation " + breakCandle.getDate() + "SL at : " + slCandle.getDate());
			List<LocalDateTime> keyDates = List.of(swingHighBefore.getDate(), breakCandle.getDate(),candleStartTrade.getDate(), slCandle.getDate());
			saveHotSpot(swingHighBefore.getDate(), candleStartTrade.getDate(), keyDates, market, timeRange, "RANGE_AUTO", null);
			break;
		};

		return indexEnd;
	}

	private double getCandleStrength(Candle c) {
		return (c.getClose() - c.getOpen())/ c.getAtr();
	}




}

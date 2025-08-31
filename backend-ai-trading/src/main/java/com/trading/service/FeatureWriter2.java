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
public class FeatureWriter2 extends AbstractService{

	private static final Integer NB_MAX_PRICE = 40;


	public Integer addFeature(List<Candle> candles, Range range, Candle breakCandle, List<Extremum> extremums3,
			String market, EnumTimeRange timeRange, String hotspotCode) throws IOException {
		Integer indexEnd = breakCandle.getIndex();
		Candle swingHighBefore = range.getSwingHighBefore();
		double nbCandlesAboveRange = 0;
		int nbCandlesUnderBreak = 0;
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
			if (previousCandle.getHigh() < breakCandle.getHigh()) {
				nbCandlesAboveRange++;
			}
			if (nbCandlesAboveRange < 4 || previousCandle.getClose() < breakCandle.getClose()) {
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
				Optional<Extremum> extremumOpt = getFirstExtremumBeforeAndLowerThan(slCandle, extremums3, Type.MIN, startTradePrice);
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
			double tp = startTradePrice + distanceOpenSl;
			double sl = slCandle.getLow();
			double rr = (tp - startTradePrice) / (startTradePrice - sl);
			if (previousCandle.getClose() > range.getMax()) {
				nbCandlesAboveRange += 1;
			}
			
			if (rr < 1) {
				increaseCount("TP < 1");
				continue;
			}
			
			boolean isTP = isTradeToTP(candleStartTrade, candles, tp, sl);
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
			mapFeature.put("f_sl_distance", distanceOpenSlAtr);
			mapFeature.put("f_accum_size", Double.valueOf(breakCandle.getIndex() - range.getIndexStart()));
			mapFeature.put("date", breakCandle.getDate());
			mapFeature.put("y", isTP ? 1d : 0d);
			//				mapFeature.put("y", new Random().nextBoolean() ? 1d : 0d);
//			addPriceFeature(mapFeature, candles, candleStartTrade.getIndex(), range);
			listMapFeatures.add(mapFeature);
//			System.out.println("New accumulation " + breakCandle.getDate() + "SL at : " + slCandle.getDate());
			List<LocalDateTime> keyDates = List.of(swingHighBefore.getDate(), breakCandle.getDate(),candleStartTrade.getDate(), slCandle.getDate());
			saveHotSpot(swingHighBefore.getDate(), candleStartTrade.getDate(), keyDates, market, timeRange, "RANGE_AUTO");
		};

		return indexEnd;
	}

	private double getCandleStrength(Candle c) {
		return (c.getClose() - c.getOpen())/ c.getAtr();
	}

	private void addPriceFeature(Map<String, Object> mapFeature, List<Candle> candles, Integer currentIndex, Range range) {
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



}

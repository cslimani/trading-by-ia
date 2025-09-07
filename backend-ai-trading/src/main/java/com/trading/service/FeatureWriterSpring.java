package com.trading.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.trading.dto.HorizontalLine;
import com.trading.dto.HotSpotData;
import com.trading.entity.Candle;
import com.trading.enums.EnumTimeRange;
import com.trading.enums.ExtremumType;
import com.trading.indicator.extremum.Extremum;

@Component
public class FeatureWriterSpring extends AbstractService{


	public Integer addFeature(List<Candle> candles, Range range, Candle breakCandle, List<Extremum> extremumsEntry,
			String market, EnumTimeRange timeRange, String hotspotCode) throws IOException {
		Integer indexEnd = breakCandle.getIndex();
		Candle swingHighBefore = range.getSwingHighBefore();
		Candle candleStartTrade = candles.get(indexEnd+1);
		double startTradePrice = candleStartTrade.getOpen();
		double distanceOpenSlAtr = 0;
		double distanceOpenSl = 0;
		int countSL = 0;

		Candle slCandle = range.getCandleMin();
//		while (distanceOpenSlAtr < 1 && countSL++ < 5) {
//			Optional<Extremum> extremumOpt = getFirstExtremumBeforeAndLowerThan(slCandle, extremumsEntry, Type.MIN, startTradePrice);
//			if (extremumOpt.isPresent()) {
//				slCandle = extremumOpt.get().getCandle();
//				distanceOpenSl = startTradePrice - slCandle.getLow();
//				distanceOpenSlAtr = distanceOpenSl/breakCandle.getAtr();
//			} else {
//				continue;
//			}
//		}
		if (countSL >=5 || slCandle.getIndex().equals(breakCandle.getIndex())) {
			increaseCount("FEATURE NO SL FOUND");
			return 0;
		}
		//			double tp = startTradePrice + distanceOpenSl;
		double slPrice = slCandle.getLow();
		double slDistance = startTradePrice - slPrice;
		if (slDistance < 0 ) {
			increaseCount("FEATURE slDistance < 0");
			return 0;
		}
		
		double tp = startTradePrice + slDistance;
		double rr = (tp - startTradePrice) / (startTradePrice - slPrice);

		if (rr < 0) {
			increaseCount("FEATURE RR < 0");
			return 0;
		}
		if (rr < 1) {
			increaseCount("FEATURE RR < 1");
			return 0;
		}
		if ( rr > 5) {
			increaseCount("FEATURE RR > 5");
			return 0;
		}

		boolean isTP = isTradeToTPBuy(candleStartTrade, candles, tp, slPrice);
		if (isTP) {
			increaseDouble("20_RR", rr);
			increaseCount("10_TP");
		} else {
			increaseDouble("20_RR", -1d);
			increaseCount("11_SL");
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
				slCandle.getDate()
//				,springCandle.getDate()
				);
		
		
		HorizontalLine lineTradeSL = HorizontalLine.builder()
				.color("#FF0000")
				.price(slCandle.getLow())
				.type("SL")
				.dateStart(slCandle.getDate())
				.build();
		HorizontalLine lineTradeTP = HorizontalLine.builder()
				.color("#00FF00")
				.price(tp)
				.type("TP")
				.dateStart(candleStartTrade.getDate())
				.build();
		HorizontalLine lineTradeStart = HorizontalLine.builder()
				.color("#FFFFFF")
				.price(startTradePrice)
				.type("START")
				.dateStart(candleStartTrade.getDate())
				.build();
		HorizontalLine lineRangeTop = HorizontalLine.builder()
				.color("#FFFFFF")
				.price(range.getMax())
				.type("TOP_RANGE")
				.dateStart(range.getDateStart())
				.build();
		HotSpotData data = HotSpotData.builder()
				.lines(List.of(lineRangeTop))
				.build();
		saveHotSpot(range.getDateStart(), candleStartTrade.getDate(), keyDates, market, timeRange, "RANGE_AUTO", data);
		return 0;
	}

	public void addFeatureSell(List<Candle> candles, Range range, Candle breakCandle, List<Extremum> extremumsEntry,
			String market, EnumTimeRange timeRange, String hotspotCode) {
		Integer indexEnd = breakCandle.getIndex();
		Candle candleStartTrade = candles.get(indexEnd+1);
		double startTradePrice = candleStartTrade.getOpen();
		int countSL = 0;
		Candle swingHighBefore = range.getSwingHighBefore();
		Candle slCandle = breakCandle;
		double distanceOpenSlAtr = 0;
		while (distanceOpenSlAtr < 1 && countSL++ < 5) {
			Optional<Extremum> extremumOpt = getFirstExtremumBeforeAndLowerThan(slCandle, extremumsEntry, ExtremumType.MAX, startTradePrice, false);
			if (extremumOpt.isPresent()) {
				slCandle = extremumOpt.get().getCandle();
				double distanceOpenSl = slCandle.getHigh() - startTradePrice;
				distanceOpenSlAtr = distanceOpenSl/breakCandle.getAtr();
			} else {
				continue;
			}
		}
		if (countSL >=5 || slCandle.getIndex().equals(breakCandle.getIndex())) {
			increaseCount("FEATURE NO SL FOUND");
			return;
		}
		//			double tp = startTradePrice + distanceOpenSl;
		double slPrice = slCandle.getHigh();
		double slDistance = slPrice - startTradePrice;
		if (slDistance < 0 ) {
			increaseCount("FEATURE slDistance < 0");
			return;
		}
		
		double tp = startTradePrice - slDistance;
		double rr = (startTradePrice - tp) / (slPrice - startTradePrice);

		if (rr < 0) {
			increaseCount("FEATURE RR < 0");
			return;
		}
		if (rr < 1) {
			increaseCount("FEATURE RR < 1");
			return;
		}
		if ( rr > 5) {
			increaseCount("FEATURE RR > 5");
			return;
		}

		boolean isTP = isTradeToTPSell(candleStartTrade, candles, tp, slPrice);
		if (isTP) {
			increaseDouble("20_RR", rr);
			increaseCount("10_TP");
		} else {
			increaseDouble("20_RR", -1d);
			increaseCount("11_SL");
		}
		List<LocalDateTime> keyDates = List.of(
				breakCandle.getDate(),
				candleStartTrade.getDate(),
				slCandle.getDate()
//				,springCandle.getDate()
				);
		HorizontalLine lineTradeSL = HorizontalLine.builder()
				.color("#FF0000")
				.price(slCandle.getHigh())
				.type("SL")
				.dateStart(slCandle.getDate())
				.build();
		HorizontalLine lineTradeTP = HorizontalLine.builder()
				.color("#00FF00")
				.price(tp)
				.type("TP")
				.dateStart(candleStartTrade.getDate())
				.build();
		HorizontalLine lineTradeStart = HorizontalLine.builder()
				.color("#FFFFFF")
				.price(startTradePrice)
				.type("START")
				.dateStart(candleStartTrade.getDate())
				.build();
		HotSpotData data = HotSpotData.builder()
				.lines(List.of(lineTradeSL, lineTradeTP, lineTradeStart))
				.build();
		saveHotSpot(swingHighBefore.getDate(), candleStartTrade.getDate(), keyDates, market, timeRange, "RANGE_AUTO", data);
	}

}

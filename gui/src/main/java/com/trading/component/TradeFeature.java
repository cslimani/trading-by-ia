package com.trading.component;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.trading.dto.TradeInfo;
import com.trading.dto.jsonb.HotSpotData;
import com.trading.entity.Candle;
import com.trading.entity.HotSpot;

@Component
public class TradeFeature extends AbstractFeature{

	boolean recording;
	Candle candleStart = null;
	Candle candleStoploss = null;

	public void mouseMoved(LocalDateTime date, Double price) {
		if (candleStart != null && candleStoploss != null && price != null) {
			double rr = buildRR(price);
			topPanel.updateIndicator("RR = " + String.format("%.1f", rr));
		}
	}

	private double buildRR(Double price) {
		return (price - candleStart.getOpen()) / (candleStart.getOpen() - candleStoploss.getLow());
	}

	public void afterCandlesLoaded() {
	}

	public void mouseClicked(LocalDateTime date, Double price) {
		if (recording) {
			topPanel.updateIndicator("");
			if (candleStart == null) {
				candleStart = data.getMapCandles().get(date);
			} else if (candleStoploss == null) {
				candleStoploss = data.getMapCandles().get(date);
			} else {
				saveTrade(date, price);
			}
		}
	}

	private void saveTrade(LocalDateTime dateEnd, Double priceEnd) {
		HotSpot hotspot = HotSpot.builder()
				.creationDate(LocalDateTime.now())
				.code("TRADE")
				.dateStart(candleStart.getDate())
				.dateEnd(dateEnd)
				.market(data.getMarketCode())
				.timeRange(data.getTimeRange())
				.data(HotSpotData.builder()
						.tradeInfo(TradeInfo.builder()
								.riskRatio(buildRR(priceEnd))
								.build())
						.build())
				.build();
		hotSpotRepository.save(hotspot);
		topPanel.updateIndicator("Trade saved RR = " + buildRR(priceEnd));
		reset();
		System.out.println("Trade created");
	}

	public void keyPressed(int keyCode) {
		//		System.out.println(keyCode);
		// T
		if (keyCode == 84) {
			recording = true;
		}
	}

	public void keyReleased(int keyCode) {
		reset();
	}

	private void reset() {
		recording = false;	
		candleStart = null;
		candleStoploss = null;
//		topPanel.updateIndicator("");
	}

}

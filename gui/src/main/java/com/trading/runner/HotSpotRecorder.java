package com.trading.runner;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.trading.entity.Candle;
import com.trading.entity.HotSpot;
import com.trading.enums.EnumTimeRange;

import lombok.extern.slf4j.Slf4j;

@Profile("hotspot-record")
@Component
@Slf4j
public class HotSpotRecorder extends AbstractRunner {

	boolean recordingEnabled;
	List<LocalDateTime> dates = new ArrayList<LocalDateTime>();
	private static final String HOTSPOT_CODE = "ACCUMULATION_EVAL";
	
	@Override
	protected void runnerInit() {
		setup("GOLD", EnumTimeRange.M5);
		dateEnd = LocalDateTime.of(2020, 1, 2, 0, 0);
//		dates.addAll(hotSpotRepository.findByCodeOrderByDateEndDesc(HOTSPOT_CODE)
//				.stream()
//				.map(hs -> hs.getDateStart())
//				.toList());
	}

	@Override
	public void keyReleased(int keyCode) {
		recordingEnabled = false;
	}

	@Override
	public void keyPressed(int keyCode) {
		//CTRL
		if (keyCode == 17) {
			recordingEnabled = true;
		}
	}

	@Override
	public void mouseClicked(LocalDateTime date, Double price) {
		String market = data.getMarketCode();
		if (recordingEnabled) {
//			Candle cStart = candleRepository.findNthCandleBeforeDate(market, EnumTimeRange.M1.name(), date, 50);
			Candle cEnd = candleRepository.findNthCandleAfterDate(market, EnumTimeRange.M1.name(), date, 50);
			hotSpotRepository.save(HotSpot.builder()
					.market(data.getMarketCode())
					.timeRange(data.getTimeRange())
					.dateStart(date)
					.dateEnd(cEnd.getDate())
					.code(HOTSPOT_CODE)
					.creationDate(LocalDateTime.now())
					.build());
			dates.add(date);
			afterPricesLoaded(candles);
			log.info("Recording one hot stop");
		}
	}
	
	@Override
	public void afterPricesLoaded(List<Candle> list) {	
		list.stream()
		.filter(c ->  dates != null && dates.contains(c.getDate()))
		.forEach(c -> {
			c.setSelected(true);
		});
	}

}

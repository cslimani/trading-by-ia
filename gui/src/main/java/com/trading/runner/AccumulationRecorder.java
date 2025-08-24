package com.trading.runner;

import java.awt.Color;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.trading.dto.jsonb.HotSpotData;
import com.trading.entity.Candle;
import com.trading.entity.HotSpot;
import com.trading.enums.EnumTimeRange;

import lombok.extern.slf4j.Slf4j;

@Profile("accumulation-record")
@Component
@Slf4j
public class AccumulationRecorder extends AbstractRunner {

	boolean recordingEnabled;
	boolean recordingNoSringEnabled;
	List<LocalDateTime> dates = new ArrayList<LocalDateTime>();
	private static final String HOTSPOT_CODE = "ACCUMULATION_RECORDED";
	private List<LocalDateTime> accumulationDates = new ArrayList<LocalDateTime>();

	@Override
	public void init() {
		super.init();
		addButton("Reset", () -> reset(), true);
	}

	private void reset() {
		dates.removeAll(accumulationDates);
		accumulationDates.clear();
		refreshTopPanel();
		applyToCandles(candles);
	}

	@Override
	protected void runnerInit() {
		setup("US100.cash", EnumTimeRange.M1);
		dateEnd = LocalDateTime.of(2025, 1, 2, 9, 0);
		dates.addAll(hotSpotRepository.findByCodeOrderByDateEndAsc(HOTSPOT_CODE)
				.stream()
				.map(hs -> hs.getKeyDates())
				.flatMap(List::stream)
				.toList());
		if (!dates.isEmpty()) {
			dateEnd =  Iterables.getLast(dates);
		}
	}

	@Override
	public void keyReleased(int keyCode) {
		recordingEnabled = false;
		recordingNoSringEnabled = false;
	}

	@Override
	public void keyPressed(int keyCode) {
		//CTRL or SHIFT
//		System.out.println(keyCode);
		if (keyCode == 17) {
			recordingEnabled = true;
		}
		if (keyCode == 16) {
			recordingNoSringEnabled = true;
		}
	}

	@Override
	public void mouseClicked(LocalDateTime date, Double price) {
		if (recordingEnabled || recordingNoSringEnabled) {
			dates.add(date);
			applyToCandles(candles);
			accumulationDates.add(date);
			refreshTopPanel();
			LocalDateTime startDate = accumulationDates.get(0);
			if (accumulationDates.size() == 4) {
				createHotSpot(startDate, accumulationDates.get(3));
			} else if (accumulationDates.size() == 3 && recordingNoSringEnabled) {
				createHotSpot(startDate, accumulationDates.get(2));
			}
		}
	}

	Candle getCandle(LocalDateTime date) {
		return data.getMapCandles().get(date);
	}
	private void createHotSpot(LocalDateTime dateStart, LocalDateTime dateEnd) {
		LocalDateTime climaxDate = accumulationDates.get(1);
		String result = null;
		Double climaxPrice = getCandle(climaxDate).getLow();
		if (getCandle(dateEnd).getLow() < climaxPrice) {
			result = "FAILURE";
		} else if (getCandle(dateEnd).getHigh() > climaxPrice) {
			result = "SUCCESS";
		} else {
			throw new RuntimeException("No result found");
		}

		hotSpotRepository.save(HotSpot.builder()
				.market(data.getMarketCode())
				.timeRange(data.getTimeRange())
				.dateStart(dateStart)
				.dateEnd(dateEnd)
				.data(HotSpotData.builder()
						.result(result)
						.spring(accumulationDates.size() == 4)
						.build())
				.keyDates(accumulationDates)
				.code(HOTSPOT_CODE)
				.creationDate(LocalDateTime.now())
				.build());
		reset();
		log.info("Recording one HotSpot");
	}

	private void refreshTopPanel() {
		if (recordingEnabled) {
			topPanel.setTradeInfo(accumulationDates.size() + "/4", Color.GREEN);
		} else if (recordingNoSringEnabled) {
			topPanel.setTradeInfo(accumulationDates.size() + "/3", Color.ORANGE);
		} else {
			topPanel.setTradeInfo("0/3", Color.WHITE);
		}
	}

	@Override
	protected void applyToCandles(List<Candle> list) {	
		list.stream()
		.filter(c ->  dates != null && dates.contains(c.getDate()))
		.forEach(c -> {
			c.setSelected(true);
		});
	}

}

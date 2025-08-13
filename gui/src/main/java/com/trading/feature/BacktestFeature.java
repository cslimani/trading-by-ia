package com.trading.feature;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.trading.dto.PeriodOfTime;
import com.trading.entity.Candle;
import com.trading.entity.Market;
import com.trading.enums.EnumDirection;
import com.trading.enums.EnumTimeRange;

@Component
public class BacktestFeature extends AbstractFeature {

	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final int MAX_candleS = 1000;
	private List<Candle> candles;
	private LocalDateTime dateStart;
	private LocalDateTime dateEnd = LocalDateTime.of(2025, 8, 1, 0, 0);
	private boolean enabledZoom;
	private List<Candle> points = new ArrayList<Candle>();
	private String currentMarket;

	public void init() {
		setEnabled(true);
		leftPanel.loadAllMarketsFromDatabase();
		dateStart = dateEnd.plusHours(-100);
		Market market = data.getMarket();
		List<Candle> candlesTmp = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(market.getCode(),EnumTimeRange.H1, dateStart, dateEnd);
		int nbTry = 0;
		while (candlesTmp.size() < 100) {
			dateStart = dateStart.plusHours(-20);
			candlesTmp = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(market.getCode(), EnumTimeRange.H1, dateStart, dateEnd);
			if (nbTry++ > 10) {
				throw new IllegalArgumentException("No candle found after 10 tries between " + dateStart+ " and " + dateEnd + " for market " + market.getCode());
			}
		}

		addButton("Expand", () -> stretchZone(30), true);
		addButton("Unzoom Expand", () -> unzoomExpand(), true);
		addButton("-50", () -> shiftBackwardBy(50), true);
		addButton("-10", () -> shiftBackwardBy(10), true);
		addButton("-1", () -> shiftBackwardBy(1), true);
		addButton("+1", () -> shiftForwardBy(1), true);
		addButton("+10", () -> shiftForwardBy(10), true);
		addButton("+50", () -> shiftForwardBy(50), true);
	}


	private void unzoomExpand() {
		stretchZone(10000);
		data.setTimeRange(EnumTimeRange.H1);
		topPanel.getTimeRangeComboBox().setSelectedItem(EnumTimeRange.H1);
		mainPanel.unzoomMaximum();
	}

	private PeriodOfTime getPeriodOfTime() {
		if (dateStart  == null || dateEnd == null) {
			return null;
		}
		return new PeriodOfTime(dateStart, dateEnd);
	}

	private void saveDateEnd(LocalDateTime dateEnd) {
		this.dateEnd = dateEnd;		
	}

	private void savePeriod(LocalDateTime dateStart, LocalDateTime dateEnd) {
		this.dateStart = dateStart;		
		this.dateEnd = dateEnd;		
	}


	public void loadData() {
		EnumTimeRange timeRange = data.getTimeRange();
		if (timeRange == null) {
			throw new RuntimeException("No timerange");
		}
		PeriodOfTime period = getPeriodOfTime();
		if (period == null) {
			System.out.println("*******************************************");
			System.out.println("Warning no pattern loaded, set default date");
			System.out.println("*******************************************");
			savePeriod(dateStart, dateEnd);
			period = getPeriodOfTime();
		}
		if (data.getDirection() == null) {
			data.setDirection(EnumDirection.BUY.name());
		}

		candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateDesc(
				data.getMarket().getCode(),
				timeRange, 
				period.getDateStart(),
				period.getDateEnd(),
				PageRequest.of(0, MAX_candleS));
		for (int i = 0; i < 10 && candles.size() < 100; i++) {
			stretchZone(10);
			period = getPeriodOfTime();
			candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateDesc(
					data.getMarket().getCode(),
					timeRange, 
					period.getDateStart(),
					period.getDateEnd(),
					PageRequest.of(0, MAX_candleS));
			System.out.println("Auto expand zone");
		}
		Collections.reverse(candles);

		candles.stream().forEach(c -> c.setColor(null));
		points.forEach(candle -> {
			candles.stream().filter(c -> c.getDate().equals(candle.getDate())).forEach(c -> c.setColor(Color.WHITE));
		});
		data.setCandles(candles);
		
		if (currentMarket == null || !currentMarket.equals(data.getMarketCode())) {
			currentMarket = data.getMarketCode();
			leftPanel.updateButtons();
		}
	}

	public void stretchZone(int value) {
		EnumTimeRange timeRange = data.getTimeRange();
		PeriodOfTime period = getPeriodOfTime();
		Pageable page = PageRequest.of(0, value);
		List<Candle> candles = candleRepository.findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(
				data.getMarket().getCode() ,timeRange, period.getDateStart(), page);
		if (candles != null && !candles.isEmpty()) {
			savePeriod(Iterables.getLast(candles).getDate(), period.getDateEnd());
		}
		mainPanel.unzoomMaximum();
	}

	private void shiftBackwardBy(int nbShift) {
		EnumTimeRange timeRange = data.getTimeRange();
		PeriodOfTime period = getPeriodOfTime();
		Pageable page = PageRequest.of(0, nbShift);
		List<Candle> nextCandles = candleRepository.findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(data.getMarket().getCode() ,timeRange, period.getDateEnd(), page);
		if (!nextCandles.isEmpty()) {
			saveDateEnd(Iterables.getLast(nextCandles).getDate());
		}
	}

	private void shiftForwardBy(int nbShift) {
		EnumTimeRange timeRange = data.getTimeRange();
		PeriodOfTime period = getPeriodOfTime();
		Pageable page = PageRequest.of(0, nbShift);
		List<Candle> nextCandles = candleRepository.findByMarketAndTimeRangeAndDateAfterOrderByDateAsc(data.getMarket().getCode(), timeRange, period.getDateEnd(), page);
		if (!nextCandles.isEmpty()) {
			saveDateEnd(Iterables.getLast(nextCandles).getDate());
		}
	}

	public List<Candle> getCandles() {
		return candles;
	}

	public void keyReleased(int keyCode) {
		enabledZoom = false;
	}

	public void keyPressed(int keyCode) {
		//ctrl
		if (keyCode == 16) {
			enabledZoom = true;
			topPanel.getTimeRangeComboBox().setSelectedItem(candles);
		}
	}

	public void onClick(LocalDateTime date, Double price) {
		if (enabledZoom) {
			EnumTimeRange timerange = EnumTimeRange.M1;
			data.setTimeRange(timerange);
			saveDateEnd(date);
			topPanel.getTimeRangeComboBox().setSelectedItem(timerange);
			mainPanel.reload(false);
		}
	}

	public void mouseMoved(LocalDateTime date, Double price) {
	}

}

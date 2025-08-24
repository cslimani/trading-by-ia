package com.trading.runner;


import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.trading.GuiFrame;
import com.trading.component.HorizontalLineFeature;
import com.trading.component.ZoneSelectorFeature;
import com.trading.dto.DataDTO;
import com.trading.dto.PeriodOfTime;
import com.trading.entity.Candle;
import com.trading.enums.EnumDirection;
import com.trading.enums.EnumTimeRange;
import com.trading.gui.LeftPanel;
import com.trading.gui.MainPanel;
import com.trading.gui.TopPanel;
import com.trading.repository.CandleRepository;
import com.trading.repository.HotSpotRepository;
import com.trading.repository.MarketRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRunner implements Runner {

	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	protected static final int MAX_CANDLES = 1000;
	protected List<Candle> candles;
	protected LocalDateTime dateStart;
	protected LocalDateTime dateEnd;
	protected List<Candle> points = new ArrayList<Candle>();
	protected String currentMarket;

	@Autowired
	public HotSpotRepository hotSpotRepository;
	@Autowired
	public DataDTO data;
	@Autowired
	public MainPanel mainPanel;
	@Autowired
	public TopPanel topPanel;
	@Autowired
	public LeftPanel leftPanel;
	@Autowired
	public GuiFrame guiFrame;
	@Autowired
	public CandleRepository candleRepository;
	@Autowired
	public ZoneSelectorFeature zoneSelectorComponent;
	@Autowired
	public ObjectMapper objectMapper;
	@Autowired 
	public PlatformTransactionManager transactionManager;
	@Autowired
	public MarketRepository marketRepository;
	@Autowired
	public HorizontalLineFeature horizontalLineComponent;


	public void transactionnal(Runnable runnable) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				runnable.run();
			}
		});
	}

	public void warn(String message) {
		topPanel.info(message, Color.red);
	}

	public void addButton(String name, Runnable runnable, boolean reload) {
		addButton(name, runnable, reload, null);
	}

	public void addButton(String name, Runnable runnable, boolean reload, boolean unzoom) {
		addButton(name, runnable, reload, unzoom, null);
	}

	public void addButton(String name, Runnable runnable, boolean reload, Runnable after) {
		addButton(name, runnable, reload, false, after);
	}
	public void addButton(String name, Runnable runnable, boolean reload, boolean unzoom, Runnable after) {
		JButton button = new JButton(name);
		topPanel.add(button);
		button.setLocation(20, 20);
		button.addActionListener(_ -> {
			runnable.run();
			mainPanel.repaint();
			topPanel.repaint();
			if (reload) {
				mainPanel.reload();
			}
			if (unzoom) {
				mainPanel.unzoomMaximum();
			}
			if (after !=null) {
				after.run();
			}
		});
	}

	public void setup(String market, EnumTimeRange tr) {
		data.setMarketCode(market);
		data.setTimeRange(tr);
		topPanel.setTimeRange(tr);
	}
	
	public void init() {
		runnerInit();
		leftPanel.loadAllMarketsFromDatabase();
		dateStart = dateEnd.plusSeconds(-100 * data.getTimeRange().getNbSeconds());
		//		dateStart = dateEnd.plusHours(-100);
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
		if (dateEnd  == null) {
			return null;
		}
		if (dateStart == null) {
			dateStart = dateEnd.plusSeconds(-100 * data.getTimeRange().getNbSeconds());
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
				PageRequest.of(0, MAX_CANDLES));
		if (candles.size() < 100) {
			stretchZone(100 - candles.size(), false);
			period = getPeriodOfTime();
			candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateDesc(
					data.getMarket().getCode(),
					timeRange, 
					period.getDateStart(),
					period.getDateEnd(),
					PageRequest.of(0, MAX_CANDLES));
		}
		Collections.reverse(candles);

		candles.stream().forEach(c -> c.setColor(null));
		points.forEach(point -> {
			candles.stream().filter(c -> c.getDate().equals(point.getDate())).forEach(c -> c.setColor(Color.WHITE));
		});
		data.setCandles(candles);
		applyToCandles(candles);
		if (currentMarket == null || !currentMarket.equals(data.getMarketCode())) {
			currentMarket = data.getMarketCode();
			leftPanel.updateButtons();
		}
	}

	protected void applyToCandles(List<Candle> list) {		
	}


	@Override
	public void postInit() {
		autoExpandIfNeeded();
	}

	private void autoExpandIfNeeded() {
		for (int i = 0; i < 10 && candles.size() < 100; i++) {
			stretchZone(10);
			PeriodOfTime period = getPeriodOfTime();
			candles = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateDesc(
					data.getMarket().getCode(),
					data.getTimeRange(), 
					period.getDateStart(),
					period.getDateEnd(),
					PageRequest.of(0, MAX_CANDLES));
			System.out.println("Auto expand zone");
		}
	}

	public void stretchZone(int value) {
		stretchZone(value, true);
	}

	public void stretchZone(int value, boolean unzoom) {
		log.info("Stretch zone of {} candles", value);
		EnumTimeRange timeRange = data.getTimeRange();
		PeriodOfTime period = getPeriodOfTime();
		Pageable page = PageRequest.of(0, value);
		List<Candle> candles = candleRepository.findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(
				data.getMarket().getCode() ,timeRange, period.getDateStart(), page);
		if (candles != null && !candles.isEmpty()) {
			savePeriod(Iterables.getLast(candles).getDate(), period.getDateEnd());
		}
		if (unzoom) {
			mainPanel.unzoomMaximum();
		}
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

	@Override
	public void keyReleased(int keyCode) {
	}

	@Override
	public void keyPressed(int keyCode) {
	}

	@Override
	public void mouseClicked(LocalDateTime date, Double price) {
	}

	
	protected abstract void runnerInit();
}

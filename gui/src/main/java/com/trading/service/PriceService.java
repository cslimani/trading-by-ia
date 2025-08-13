package com.trading.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.trading.GuiFrame;
import com.trading.component.HorizontalLineComponent;
import com.trading.dto.DataDTO;
import com.trading.dto.GraphDateDTO;
import com.trading.entity.Candle;
import com.trading.enums.EnumMode;
import com.trading.feature.BacktestFeature;
import com.trading.gui.MainPanel;
import com.trading.gui.TopPanel;
import com.trading.repository.CandleRepository;
import com.trading.repository.MarketRepository;

import jakarta.annotation.PostConstruct;

@Component
public class PriceService {

	private static final int EXTRA_WIDTH = 50;
	public static Logger logger = LoggerFactory.getLogger(PriceService.class);

	@Autowired
	MarketRepository marketRepository;
	@Autowired
	CandleRepository candleRepository;
	@Autowired
	DataDTO data;
	@Autowired
	TopPanel topPanel;
	@Autowired
	BacktestFeature backtestFeature;
	@Autowired
	HorizontalLineComponent horizontalLineComponent;

	@PostConstruct
	public void init() {
	}

	public void init(int panelWidth) {
		horizontalLineComponent.setEnabled(true);
		if (data.getMode() == EnumMode.LIVE) {
		} else if (data.getMode() == EnumMode.ACCUMULATION) {
		} else if (data.getMode() == EnumMode.BACKTEST) {
			backtestFeature.init();
		}
		loadData(panelWidth);
	}

	public void loadData(int panelWidth) {
		if (data.getMode() == EnumMode.ACCUMULATION) {
		} else if (data.getMode() == EnumMode.BACKTEST) {
			backtestFeature.loadData();
		} else if (data.getMode() == EnumMode.LIVE) {
		}
		data.setDateStart(data.getCandles().get(0).getDate());
		List<Candle> candles = data.getCandles();
		data.getMapCandles().clear();
		candles.forEach(c -> {
			data.getMapCandles().put(c.getDate(), c);
			c.setEndDate(c.getDate().plusSeconds(c.getTimeRange().getNbSeconds()).minusNanos(1));
		});
		IntStream.range(0, candles.size()).forEach(p -> candles.get(p).setIndex(p));
		this.rebuildGraphDates(panelWidth, false);
	}


	public long getSecondsFromEpoch(LocalDateTime date) {
		ZoneId zoneId = ZoneId.systemDefault();
		return date.atZone(zoneId).toEpochSecond();
	}

	public synchronized void zoomIn() {
		if (data.getSpaceBetweenCandles() < GuiFrame.SPACE_BETWEEN_candleS) {
			data.setSpaceBetweenCandles(data.getSpaceBetweenCandles() + 1);
		} else {
			data.setCandleWidth(data.getCandleWidth() + 1);
		}
	}

	public synchronized void zoomOut() {
		if (data.getCandleWidth() > 1) {
			data.setCandleWidth(data.getCandleWidth() - 1);
		} else if (data.getSpaceBetweenCandles() > 1) {
			data.setSpaceBetweenCandles(data.getSpaceBetweenCandles() - 1);
		}
	}


	public int rebuildGraphDates(int panelWidth, boolean perfectMatch) {
		int viewTotalWidth = 0;
		if (panelWidth == 0) {
			panelWidth = MainPanel.WINDOW_WIDTH;
		}
		data.setSpaceBetweenCandles(data.getSpaceBetweenCandles());
		List<GraphDateDTO> graphDateDTOs = new ArrayList<GraphDateDTO>();
		int graphDateWidth = data.getCandleWidth() + data.getSpaceBetweenCandles();

		int extraWidth = EXTRA_WIDTH;
		if (perfectMatch) {
			graphDateWidth = panelWidth / data.getCandles().size();
			int candleWidth = graphDateWidth - data.getSpaceBetweenCandles();
			data.setCandleWidth(candleWidth);
			extraWidth = panelWidth - data.getCandles().size()*graphDateWidth;
		}

		GraphDateDTO lastGraphDate = null;
		GraphDateDTO firstGraphDate = null;
		for (Candle candle : data.getCandles()) {
			GraphDateDTO graphDateDTO = new GraphDateDTO(candle, candle.getDate(), candle.getEndDate(), viewTotalWidth, viewTotalWidth + graphDateWidth - 1);
			graphDateDTOs.add(graphDateDTO);
			lastGraphDate = graphDateDTO;
			if (firstGraphDate == null) {
				firstGraphDate = graphDateDTO;
			}
			viewTotalWidth = viewTotalWidth + graphDateWidth;
		}	
		data.setGraphDates(graphDateDTOs);
		data.setLastGraphDate(lastGraphDate);
		data.setFirstGraphDate(firstGraphDate);
		data.setViewTotalWidth(viewTotalWidth + extraWidth);

		return viewTotalWidth;
	}

	public int getViewTotalWidth(){
		int viewTotalWidth = 0;
		int graphDateWidth = data.getCandleWidth() + data.getSpaceBetweenCandles();
		for (Candle _ : data.getCandles()) {
			viewTotalWidth = viewTotalWidth + graphDateWidth;
		}	
		return viewTotalWidth+ EXTRA_WIDTH;
	}


//	@Transactional
//	public void synchroPricesWithRemote(String market) {
//		if (market == null) {
//			System.out.println("Market is null");
//			return;
//		}
//		TickPrice lastTick = tickPriceRepository.findFirstByMarketOrderByDateDesc(market);
//		LocalDateTime date = null;
//		if (lastTick != null) {
//			date = lastTick.getDate();
//		} else {
//			date = LocalDateTime.of(2023, 6, 1, 0, 0);
//		}
//		long time = System.currentTimeMillis();
//		List<TickPrice> pricesAfterLast = jdbcService.getTickPricesAfter(market, date)
//				.stream()
//				.filter(t -> t.getAsk() != null || t.getBid() != null)
//				.toList();
//		//		System.out.println(market + " - Prices found " + pricesAfterLast.size() + " - time " + (System.currentTimeMillis() - time)/1000);
//		if (!pricesAfterLast.isEmpty()) {
//			tickPriceRepository.saveAll(pricesAfterLast);
//			Arrays.stream(EnumTimeRange.values()).parallel().forEach(tr -> {
//				long timee = System.currentTimeMillis();
//				List<Candle> newCandles = convertTicksToCandles(pricesAfterLast, tr, market);
//				//									System.out.println(market + " - Time to convert " + (System.currentTimeMillis() - timee)/1000 + " " + tr);
//				timee = System.currentTimeMillis();
//				List<Candle> candlesAlreadyExist = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(
//						market, 
//						tr, 
//						newCandles.get(0).getDate(),
//						newCandles.get(newCandles.size()-1).getDate());
//				candlesAlreadyExist.forEach(candleRepository::delete);
//				candleRepository.saveAll(newCandles);
//				//				System.out.println(market + " - Time to save " + (System.currentTimeMillis() - timee)/1000 + " " + tr);
//			});
//			System.out.println("Synchro finished for " + pricesAfterLast.size() + " prices for market " + market );
//		}
//		//		List<TickPrice> allTicks = tickPriceRepository.findByMarketAndDateBetweenOrderByDate(market, LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2024, 1, 1, 0, 0));
//		//		Arrays.stream(EnumTimeRange.values()).forEach(tr -> {
//		//			List<Candle> newCandles = convertTicksToCandles(allTicks, tr, market);
//		//			candleRepository.saveAll(newCandles);
//		//		});
//		//		System.out.println("Synchro candles finished for market " + market);
//	}



	
}

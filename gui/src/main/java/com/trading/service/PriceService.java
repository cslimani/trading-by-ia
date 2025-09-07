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
import com.trading.component.DistanceFeature;
import com.trading.component.HorizontalLineFeature;
import com.trading.component.IndicatorFeature;
import com.trading.dto.DataDTO;
import com.trading.dto.GraphDateDTO;
import com.trading.entity.Candle;
import com.trading.entity.Market;
import com.trading.gui.MainPanel;
import com.trading.gui.TopPanel;
import com.trading.repository.CandleRepository;
import com.trading.repository.MarketRepository;
import com.trading.runner.Runner;

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
	Runner runner;
	@Autowired
	HorizontalLineFeature horizontalLineComponent;
	@Autowired
	IndicatorFeature indicatorFeature;
	@Autowired
	DistanceFeature distanceFeature;

	@PostConstruct
	public void init() {
	}

	public void init(int panelWidth) {
		horizontalLineComponent.setEnabled(true);
		runner.init();
		Market market = marketRepository.getByCode(data.getMarketCode());
		data.setMarket(market);
		loadData(panelWidth);
	}

	public void loadData(int panelWidth) {
		runner.loadData();
		List<Candle> candles = data.getCandles();
		IntStream.range(0, candles.size()).forEach(p -> candles.get(p).setIndex(p));
		indicatorFeature.afterCandlesLoaded();
		distanceFeature.afterCandlesLoaded();
		data.setDateStart(data.getCandles().get(0).getDate());
		data.getMapCandles().clear();
		candles.forEach(c -> {
			data.getMapCandles().put(c.getDate(), c);
			c.setEndDate(c.getDate().plusSeconds(c.getTimeRange().getNbSeconds()).minusNanos(1));
		});
		runner.afterPricesLoaded(candles);
		this.rebuildGraphDates(panelWidth, false);
	}


	public long getSecondsFromEpoch(LocalDateTime date) {
		ZoneId zoneId = ZoneId.systemDefault();
		return date.atZone(zoneId).toEpochSecond();
	}

	public synchronized void zoomIn() {
		if (data.getSpaceBetweenCandles() < GuiFrame.SPACE_BETWEEN_CANDLES) {
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

	public void postInit(int panelWidth) {
		runner.postInit();
		loadData(panelWidth);		
	}

	

}

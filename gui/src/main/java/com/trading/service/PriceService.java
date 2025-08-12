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
import com.trading.entity.Candel;
import com.trading.entity.Market;
import com.trading.entity.TickPrice;
import com.trading.enums.EnumMode;
import com.trading.enums.EnumTimeRange;
import com.trading.feature.AccumulationFeature;
import com.trading.feature.BacktestFeature;
import com.trading.gui.MainPanel;
import com.trading.gui.TopPanel;
import com.trading.repository.CandelRepository;
import com.trading.repository.MarketRepository;
import com.trading.repository.PointOfInterestRespository;
import com.trading.repository.TickPriceRepository;
import com.trading.repository.TrainablePatternRepository;
import com.trading.utils.DateHelper;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

@Component
public class PriceService {

	private static final int EXTRA_WIDTH = 50;
	public static Logger logger = LoggerFactory.getLogger(PriceService.class);

	@Autowired
	PointOfInterestRespository poiRespository;
	@Autowired
	TrainablePatternRepository trainablePatternRepository;
	@Autowired
	JdbcService jdbcService;
	@Autowired
	MarketRepository marketRepository;
	@Autowired
	CandelRepository candelRepository;
	@Autowired
	DataDTO data;
	@Autowired
	TopPanel topPanel;
	@Autowired
	BacktestFeature backtestFeature;
	@Autowired
	AccumulationFeature accumulationFeature;
	@Autowired
	TickPriceRepository tickPriceRepository;
	@Autowired
	HorizontalLineComponent horizontalLineComponent;

	@PostConstruct
	public void init() {
	}

	public void init(int panelWidth) {
		horizontalLineComponent.setEnabled(true);
		if (data.getMode() == EnumMode.LIVE) {
		} else if (data.getMode() == EnumMode.ACCUMULATION) {
			accumulationFeature.init();
		} else if (data.getMode() == EnumMode.BACKTEST) {
			backtestFeature.init();
		}
		loadData(panelWidth);
	}

	public void loadData(int panelWidth) {
		if (data.getMode() == EnumMode.ACCUMULATION) {
			accumulationFeature.loadData();
		} else if (data.getMode() == EnumMode.BACKTEST) {
			backtestFeature.loadData();
		} else if (data.getMode() == EnumMode.LIVE) {
		}
		data.setDateStart(data.getCandels().get(0).getDate());
		List<Candel> candels = data.getCandels();
		data.getMapCandels().clear();
		candels.forEach(c -> {
			data.getMapCandels().put(c.getDate(), c);
			c.setEndDate(c.getDate().plusSeconds(c.getTimeRange().getNbSeconds()).minusNanos(1));
		});
		IntStream.range(0, candels.size()).forEach(p -> candels.get(p).setIndex(p));
		this.rebuildGraphDates(panelWidth, false);
	}


	public long getSecondsFromEpoch(LocalDateTime date) {
		ZoneId zoneId = ZoneId.systemDefault();
		return date.atZone(zoneId).toEpochSecond();
	}

	public synchronized void zoomIn() {
		if (data.getSpaceBetweenCandels() < GuiFrame.SPACE_BETWEEN_CANDELS) {
			data.setSpaceBetweenCandels(data.getSpaceBetweenCandels() + 1);
		} else {
			data.setCandelWidth(data.getCandelWidth() + 1);
		}
	}

	public synchronized void zoomOut() {
		if (data.getCandelWidth() > 1) {
			data.setCandelWidth(data.getCandelWidth() - 1);
		} else if (data.getSpaceBetweenCandels() > 1) {
			data.setSpaceBetweenCandels(data.getSpaceBetweenCandels() - 1);
		}
	}


	public int rebuildGraphDates(int panelWidth, boolean perfectMatch) {
		int viewTotalWidth = 0;
		if (panelWidth == 0) {
			panelWidth = MainPanel.WINDOW_WIDTH;
		}
		data.setSpaceBetweenCandels(data.getSpaceBetweenCandels());
		List<GraphDateDTO> graphDateDTOs = new ArrayList<GraphDateDTO>();
		int graphDateWidth = data.getCandelWidth() + data.getSpaceBetweenCandels();

		int extraWidth = EXTRA_WIDTH;
		if (perfectMatch) {
			graphDateWidth = panelWidth / data.getCandels().size();
			int candelWidth = graphDateWidth - data.getSpaceBetweenCandels();
			data.setCandelWidth(candelWidth);
			extraWidth = panelWidth - data.getCandels().size()*graphDateWidth;
		}

		GraphDateDTO lastGraphDate = null;
		GraphDateDTO firstGraphDate = null;
		for (Candel candel : data.getCandels()) {
			GraphDateDTO graphDateDTO = new GraphDateDTO(candel, candel.getDate(), candel.getEndDate(), viewTotalWidth, viewTotalWidth + graphDateWidth - 1);
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
		int graphDateWidth = data.getCandelWidth() + data.getSpaceBetweenCandels();
		for (Candel candel : data.getCandels()) {
			viewTotalWidth = viewTotalWidth + graphDateWidth;
		}	
		return viewTotalWidth+ EXTRA_WIDTH;
	}

	@Transactional
	public void moveBacktestDateForward() {
		Market m = marketRepository.getByCode(data.getMarket().getCode());
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
//				List<Candel> newCandels = convertTicksToCandels(pricesAfterLast, tr, market);
//				//									System.out.println(market + " - Time to convert " + (System.currentTimeMillis() - timee)/1000 + " " + tr);
//				timee = System.currentTimeMillis();
//				List<Candel> candelsAlreadyExist = candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(
//						market, 
//						tr, 
//						newCandels.get(0).getDate(),
//						newCandels.get(newCandels.size()-1).getDate());
//				candelsAlreadyExist.forEach(candelRepository::delete);
//				candelRepository.saveAll(newCandels);
//				//				System.out.println(market + " - Time to save " + (System.currentTimeMillis() - timee)/1000 + " " + tr);
//			});
//			System.out.println("Synchro finished for " + pricesAfterLast.size() + " prices for market " + market );
//		}
//		//		List<TickPrice> allTicks = tickPriceRepository.findByMarketAndDateBetweenOrderByDate(market, LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2024, 1, 1, 0, 0));
//		//		Arrays.stream(EnumTimeRange.values()).forEach(tr -> {
//		//			List<Candel> newCandels = convertTicksToCandels(allTicks, tr, market);
//		//			candelRepository.saveAll(newCandels);
//		//		});
//		//		System.out.println("Synchro candels finished for market " + market);
//	}


	public List<Candel> convertTicksToCandels(List<TickPrice> ticks, EnumTimeRange timeRange, String market) {
		List<Candel> candels = new ArrayList<>();
		Candel currentCandel = null;
		for (TickPrice tickPrice : ticks) {
			LocalDateTime date = tickPrice.getDate();
			if (tickPrice.getAsk() == null && tickPrice.getBid() == null) {
				continue;
			}
			LocalDateTime truncatedDate = DateHelper.getTruncatedDate(date, timeRange);

			if (currentCandel == null || !currentCandel.getDate().isEqual(truncatedDate)) {
				currentCandel = new Candel();
				candels.add(currentCandel);
				currentCandel.setMarket(market);
				currentCandel.setTimeRange(timeRange);
				currentCandel.setDate(truncatedDate);
				//Bid
				currentCandel.setLowBid(tickPrice.getBid());
				currentCandel.setHighBid(tickPrice.getBid());
				currentCandel.setOpenBid(tickPrice.getBid());
				currentCandel.setCloseBid(tickPrice.getBid());
				//Ask
				currentCandel.setLowAsk(tickPrice.getAsk());
				currentCandel.setHighAsk(tickPrice.getAsk());
				currentCandel.setOpenAsk(tickPrice.getAsk());
				currentCandel.setCloseAsk(tickPrice.getAsk());
			} else {
				updateCandel(currentCandel, tickPrice);
			}
		}
		return candels;
	}

	public void updateCandel(Candel candel, TickPrice tickPrice) {
		if (tickPrice.getBid() != null) {
			if (candel.getOpenBid() == null) {
				candel.setOpenBid(tickPrice.getBid());
			}
			candel.setCloseBid(tickPrice.getBid());
			if (candel.getHighBid() != null) {
				candel.setHighBid(Math.max(candel.getHighBid(), tickPrice.getBid()));
				candel.setLowBid(Math.min(candel.getLowBid(), tickPrice.getBid()));
			} else {
				candel.setHighBid(tickPrice.getBid());
				candel.setLowBid(tickPrice.getBid());
			}
		}

		if (tickPrice.getAsk() != null) {
			if (candel.getOpenAsk() == null) {
				candel.setOpenAsk(tickPrice.getAsk());
			}
			candel.setCloseAsk(tickPrice.getAsk());
			if (candel.getHighAsk() != null) {
				candel.setHighAsk(Math.max(candel.getHighAsk(), tickPrice.getAsk()));
				candel.setLowAsk(Math.min(candel.getLowAsk(), tickPrice.getAsk()));
			} else {
				candel.setHighAsk(tickPrice.getAsk());
				candel.setLowAsk(tickPrice.getAsk());
			}
		}
	}

	
}

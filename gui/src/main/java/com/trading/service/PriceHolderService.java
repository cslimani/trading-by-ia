package com.trading.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.trading.dto.DataDTO;
import com.trading.entity.Candel;
import com.trading.entity.Market;
import com.trading.entity.TickPrice;
import com.trading.enums.EnumTimeRange;
import com.trading.feature.PriceUpdateListener;
import com.trading.gui.LeftPanel;
import com.trading.gui.TopPanel;
import com.trading.repository.CandelRepository;
import com.trading.repository.MarketRepository;
import com.trading.repository.TickPriceRepository;
import com.trading.utils.DateHelper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PriceHolderService {

	@Autowired
	JdbcService jdbcService;
	@Autowired
	MarketRepository marketRepository;
	@Autowired
	CandelRepository candelRepository;
	@Autowired
	TickPriceRepository tickPriceRepository;
	@Autowired
	PriceService priceService;
	@Autowired
	TopPanel topPanel;
	@Autowired
	LeftPanel leftPanel;
	ExecutorService initPool = Executors.newFixedThreadPool(2);
	List<String> activeMarkets;
	@Autowired
	DataDTO data;

	ConcurrentLinkedQueue<TickPrice> waitingTicksToBeProcessed = new ConcurrentLinkedQueue<>();
	volatile boolean ready = false;
	List<PriceUpdateListener> listeners = new ArrayList<>();

	Map<String,Map<EnumTimeRange, Map<LocalDateTime,Candel>>> mapMarkets = new ConcurrentHashMap<>();

	public void addListener(PriceUpdateListener listener) {
		listeners.add(listener);
	}

	public void init() {
		activeMarkets = marketRepository.findByEnabled(true)
				.stream()
				.map(m -> m.getCode())
				.toList();
//		activeMarkets = List.of(activeMarkets.get(0), activeMarkets.get(1));
		activeMarkets.parallelStream().map(marketCode -> 
		CompletableFuture.runAsync(() -> {
			Market market = marketRepository.getByCode(marketCode);
			Map<EnumTimeRange, Map<LocalDateTime,Candel>> mapTimerange = new HashMap<>();
			mapMarkets.put(marketCode, mapTimerange);
//						List.of(EnumTimeRange.M1).forEach(tr -> {
			Arrays.stream(EnumTimeRange.values()).parallel().forEach(tr -> {
				List<Candel> candels = candelRepository.findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(
						marketCode,
						tr,
						LocalDateTime.now(),
						PageRequest.of(0, 600));
				Collections.reverse(candels);
				addCandelsMissingFromTicks(marketCode, tr, candels);
				mapTimerange.put(tr, candels.stream().collect(Collectors.toMap(c -> c.getDate(), c -> c)));
				System.out.println("Market " + marketCode + " init OK - last candel " + candels.get(candels.size()-1).getDate());
			});
			synchronized (topPanel) {
				data.getMapMarkets().put(marketCode, market);
				data.setMarket(market);
				leftPanel.reloadMarkets();
			}
		}, initPool)).forEach(f -> f.join());
		ready = true;
		new Thread(() -> runProcessing()).start();
	}

	private void addCandelsMissingFromTicks(String market, EnumTimeRange tr, List<Candel> candels) {
		Candel lastCandel = candels.get(candels.size()-1);
		LocalDateTime date = lastCandel.getDate();
		candels.remove(lastCandel);
		List<TickPrice> ticks = tickPriceRepository.findByMarketAndDateAfterOrderByDate(market, date);
		if (!ticks.isEmpty()) {
			List<Candel> newCandels = priceService.convertTicksToCandels(ticks, tr, market);
			candels.addAll(newCandels);
		}
	}

	private void runProcessing() {
		while (true) {
			if (ready) {
				TickPrice tick;
				while ((tick = waitingTicksToBeProcessed.poll()) != null) {
					process(tick);
				}
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void addTickToProcess(TickPrice tick) {
		waitingTicksToBeProcessed.add(tick);
	}

	private void process(TickPrice tick) {
		if (activeMarkets.contains(tick.getMarket())) {
			//			System.out.println("process " + tick.getDate());
			Arrays.stream(EnumTimeRange.values()).parallel().forEach(tr -> {
				LocalDateTime truncatedDate = DateHelper.getTruncatedDate(tick.getDate(), tr);
				Map<EnumTimeRange, Map<LocalDateTime, Candel>> mapTimerange = mapMarkets.get(tick.getMarket());
				if (mapTimerange.get(tr) != null) {
					Candel candel = mapTimerange.get(tr).get(truncatedDate);
					if (candel == null) {
						candel = createCandel(tick, tr, truncatedDate);
						mapTimerange.get(tr).put(truncatedDate, candel);
					} else {
						priceService.updateCandel(candel, tick);
					}
					final Candel candelFinal = candel;
					listeners.forEach(l -> l.onUpdatePrice(candelFinal, tr));
				}
			});
		}
	}

	private Candel createCandel(TickPrice tick, EnumTimeRange tr, LocalDateTime date) {
		Candel candel = new Candel();
		candel.setMarket(tick.getMarket());
		candel.setTimeRange(tr);
		candel.setDate(date);
		//Bid
		candel.setLowBid(tick.getBid());
		candel.setHighBid(tick.getBid());
		candel.setOpenBid(tick.getBid());
		candel.setCloseBid(tick.getBid());

		//Ask
		candel.setLowAsk(tick.getAsk());
		candel.setHighAsk(tick.getAsk());
		candel.setOpenAsk(tick.getAsk());
		candel.setCloseAsk(tick.getAsk());

		return candel;
	}

	public List<Candel> getCandels(String marketCode, EnumTimeRange timeRange) {
		return new CopyOnWriteArrayList<Candel>( mapMarkets.get(marketCode).get(timeRange).values()
				.stream()
				.sorted(Comparator.comparing(c -> c.getDate()))
				.toList());
	}

	public void verifyCandels() {
		mapMarkets.values().forEach(mapTimerange -> {
			List<Candel> candelsNotValidated = mapTimerange.get(EnumTimeRange.M1).values().stream().filter(c -> !BooleanUtils.isTrue(c.getVerified())).toList();
			if (!candelsNotValidated.isEmpty()) {
				log.debug("{} - there are {} not validated candels starts at {}",candelsNotValidated.get(0).getMarket()
						,candelsNotValidated.size() , candelsNotValidated.get(0).getDate());
				candelsNotValidated.forEach(candelNotVerified -> {
					Optional<Candel> candelDatabaseOpt = candelRepository.findById(candelNotVerified.buildId());
					if (candelDatabaseOpt.isPresent()) {
						if (candelDatabaseOpt.get().isVerified()) {
							Candel candelDatabase = candelDatabaseOpt.get();
							if (candelNotVerified.equalsPrices(candelDatabaseOpt.get())) {
								candelNotVerified.setVerified(true);
							} else {
								log.info("Candel is not valid compared with database {}, fixing it", candelNotVerified.buildId());
								candelNotVerified.setCloseAsk(candelDatabase.getCloseAsk());
								candelNotVerified.setCloseBid(candelDatabase.getCloseBid());
								candelNotVerified.setHighAsk(candelDatabase.getHighAsk());
								candelNotVerified.setHighBid(candelDatabase.getHighBid());
								candelNotVerified.setLowAsk(candelDatabase.getLowAsk());
								candelNotVerified.setLowBid(candelDatabase.getLowBid());
								candelNotVerified.setOpenAsk(candelDatabase.getOpenAsk());
								candelNotVerified.setOpenBid(candelDatabase.getOpenBid());
								if (!candelNotVerified.equalsPrices(candelDatabaseOpt.get())) {
									log.warn("Not valid after fixing, should never happen");
								}
							}
						}
					} else {
						//						log.warn("Candel does not exist in database " + candelNotVerified.buildId());
					}
				});
			}
		});
	}
}

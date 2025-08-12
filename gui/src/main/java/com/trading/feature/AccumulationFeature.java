package com.trading.feature;

import java.awt.Color;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Iterables;
import com.trading.dto.AccumulationContent;
import com.trading.dto.PeriodOfTime;
import com.trading.entity.Accumulation;
import com.trading.entity.AccumulationId;
import com.trading.entity.Candel;
import com.trading.entity.Market;
import com.trading.entity.TrainingTrade;
import com.trading.enums.EnumDirection;
import com.trading.enums.EnumTimeRange;
import com.trading.indicator.Indicator;
import com.trading.indicator.RSI;
import com.trading.indicator.TryIndicatorSurface;
import com.trading.service.AccumulationService;
import com.trading.utils.AppUtils;

@Component
public class AccumulationFeature extends AbstractFeature {

	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final int MAX_CANDELS = 1000;
	private List<Candel> candels;
	private LocalDateTime dateStart;
	private LocalDateTime dateEnd = LocalDateTime.of(2023, 9, 1, 0, 0);
	private boolean enabledAccumulation;
	private boolean enabledZoom;
	private List<Candel> points = new ArrayList<Candel>();
	private List<Accumulation> allAccumulations;
	private Integer indexAcc = 0;
	private Accumulation acc;
	private Map<EnumTimeRange, Indicator> mapIndicator = new HashMap<>();
	private Map<EnumTimeRange, Indicator> mapRSI = new HashMap<>();
	private Object currentMarket;

	@Autowired
	AccumulationService accumulationService;

	public void init() {
		setEnabled(true);
		leftPanel.loadAllMarketsFromDatabase();
		//		Accumulation latestAcc = accumulationRespository.findFirstByMarketAndStatusOrderByDateEndDesc(data.getMarket().getCode(), "OK");
		//		if (latestAcc != null) {
		//			System.out.println("Last ACC  at " + latestAcc.getDateEnd());
		//			dateEnd = latestAcc.getDateEnd();
		//		}
		dateStart = dateEnd.plusHours(-100);
		Market market = data.getMarket();
		List<Candel> candelsTmp = candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(market.getCode(),EnumTimeRange.H1, dateStart, dateEnd);
		int nbTry = 0;
		while (candelsTmp.size() < 100) {
			dateStart = dateStart.plusHours(-20);
			candelsTmp = candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(market.getCode(), EnumTimeRange.H1, dateStart, dateEnd);
			if (nbTry++ > 10) {
				throw new IllegalArgumentException("No candel found after 10 tries");
			}
		}

		//		allAccumulations = accumulationRespository.findByMarketAndStatusAndTypeOrderByDateEndAsc(data.getMarket().getCode(), "OK", "SPRING");

		//		System.out.println(allAccumulations.size() + " accumulations found from database");
				
		//		Optional<TrainingTrade> lastTrade = trainingTradeRepository.findFirstByMarketOrderByDateCloseDesc(data.getMarketCode());
		//		LocalDateTime date;
		//		if (lastTrade.isPresent()) {
		//			date = lastTrade.get().getDateClose();
		//		} else {
		//			date = LocalDateTime.of(2015, 1, 10, 0, 0);
		//		}
		//		allAccumulations = accumulationService.getAccumulationsFromIndicator(tr, market.getCode(), mapIndicator.get(tr))
		//				.stream().filter(t -> t.getDateEnd().isAfter(date)).toList();
		//		System.out.println(allAccumulations.size() + " accumulations found from indicators");

		//		allAccumulations = allAccumulations.stream()
		//				.filter(a -> a.getTimeRange() == EnumTimeRange.M1)
		//				//				.filter(a -> !containsInAccumulation(a, accumulationsFromIndicator))
		//				.toList();
		//		allAccumulations = accumulationsFromIndicator;

		dateStart = LocalDateTime.of(2023, 9, 11, 0, 0);
		dateEnd = LocalDateTime.of(2023, 9, 13, 0, 0);
		EnumTimeRange tr = EnumTimeRange.S15;
//		new Thread(() -> buildIndicatorMap(dateStart.minusDays(7), dateEnd)).start();
//		waitForIndicatorReady(tr);
		data.setMarket(marketRepository.getByCode("XAUUSD"));
		allAccumulations = accumulationService.getAccumulationWithBOS(
				data.getMarketCode(),
				tr,
				dateStart,
				dateEnd,
				mapIndicator.get(tr));
		System.out.println(allAccumulations.size() + " total accumulations found ");

		addButton("ST", () -> saveAcc("ST"), true);
		addButton("Spring", () -> saveAcc("SPRING"), true);
		addButton("FAILURE", () -> saveAcc("FAILURE"), true);
		addButton("INVALID", () -> saveAcc("INVALID"), true);
		addButton("X", () -> deleteAcc(), true);
		addButton("<", () -> previousAcc(), true);
		addButton(">", () -> nextAcc(), true);
		addButton("Clear", () -> points.clear(), true);
		addButton("Expand", () -> stretchZone(30), true);
		addButton("Unzoom Expand", () -> unzoomExpand(), true);
		//		addButton("Capture Zone", () -> captureZone(), true);
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


	private void saveAcc(String type) {
		acc.setType(type);
		accumulationRespository.save(acc);
		nextAcc();
		System.out.println("Accumulation saved in database");
	}


	private boolean containsInAccumulation(Accumulation a, List<Accumulation> accumulationsFromIndicator) {
		try {
			a.setContent(objectMapper.readValue(a.getContentJson(), AccumulationContent.class));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		waitForIndicatorReady(a.getTimeRange());

		PageRequest page = PageRequest.of(0, 100);
		List<Candel> candelsBeforeAccu = candelRepository.findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(
				data.getMarket().getCode(),
				a.getTimeRange(),
				a.getContent().getPoints().get(0),
				page);
		return candelsBeforeAccu
				.stream()
				.anyMatch(c -> mapIndicator.get(a.getTimeRange()).get(c.getDate()) > 35);
		//		return accumulationsFromIndicator.stream()
		//				.anyMatch(a1 -> {
		//					return Math.abs(Duration.between(a.getContent().getPoints().get(0), a1.getContent().getPoints().get(0)).toMinutes()) < 100 ||
		//							Math.abs(Duration.between(a.getContent().getPoints().get(0), a1.getContent().getPoints().get(3)).toMinutes()) < 100;
		//				});

	}


	private void waitForIndicatorReady(EnumTimeRange timeRange) {
		while (mapIndicator.get(timeRange) == null) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


	private void buildIndicatorMap(LocalDateTime dateStart, LocalDateTime dateEnd) {
		Arrays.stream(EnumTimeRange.values()).parallel().forEach( tr -> {
			List<Candel> candelsIndicator = 
					candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(
							data.getMarketCode(), 
							tr,
							dateStart, 
							dateEnd);
			IntStream.range(0, candelsIndicator.size()).boxed().forEach(i -> candelsIndicator.get(i).setIndex(i));

			Indicator indicator = new TryIndicatorSurface(candelsIndicator, 15, tr);
			mapIndicator.put(tr, indicator);

			//			allAccumulations.stream()
			//			.filter(a -> a.getTimeRange() == tr)
			//			.forEach(a -> {
			//				try {
			//					a.setContent(objectMapper.readValue(a.getContentJson(), AccumulationContent.class));
			//				} catch (JsonProcessingException e) {
			//					e.printStackTrace();
			//				}
			//				System.out.println(indicator.get(a.getContent().getPoints().get(0)));
			//			});
		});

//		Arrays.stream(EnumTimeRange.values()).parallel().forEach( tr -> {
//			List<Candel> candelsIndicator = 
//					candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(
//							data.getMarketCode(), 
//							tr,
//							LocalDateTime.of(2015, 1, 1, 0, 0), 
//							LocalDateTime.of(2016, 1, 1, 0, 0));
//			IntStream.range(0, candelsIndicator.size()).boxed().forEach(i -> candelsIndicator.get(i).setIndex(i));
//
//			Indicator indicator = new RSI(candelsIndicator, 14);
//			mapRSI.put(tr, indicator);
//		});
	}




	private void deleteAcc() {
		acc.setStatus("DELETED");
		accumulationRespository.save(acc);
	}

	private void nextAcc() {
		if (indexAcc < allAccumulations.size()-1) {
			indexAcc++;
		}
		loadAcc();
	}

	private void previousAcc() {
		if (indexAcc > 0) {
			indexAcc--;
		}
		loadAcc();
	}

	private void loadAcc() {
		acc = allAccumulations.get(indexAcc);
		topPanel.setNbCandels(indexAcc);
		try {
			EnumTimeRange timerange = acc.getTimeRange();
			data.setTimeRange(timerange);
			topPanel.getTimeRangeComboBox().setSelectedItem(timerange);
			points.clear();
			if (acc.getContent() == null && acc.getContentJson() != null) {
				acc.setContent(objectMapper.readValue(acc.getContentJson(), AccumulationContent.class));
			} 
			if (acc.getContent() != null) {
				List<Candel> candels = candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(
						data.getMarket().getCode(),
						timerange,
						acc.getContent().getPoints().get(0),
						acc.getContent().getPoints().get(3));
				List<Candel> candelsBefore = candelRepository.findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(
						data.getMarket().getCode(),
						timerange,
						acc.getContent().getPoints().get(0),
						PageRequest.of(0, 500));
				Collections.reverse(candelsBefore);
				if (candels != null && !candels.isEmpty()) {
					savePeriod(candelsBefore.get(0).getDate(), Iterables.getLast(candels).getDate());
				}
				candelsBefore.addAll(candels);
			} else {
				List<Candel> candelsBefore = candelRepository.findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(
						data.getMarket().getCode(),
						timerange,
						acc.getDateEnd(),
						PageRequest.of(0, 300));
				Collections.reverse(candelsBefore);
				if (candels != null && !candels.isEmpty()) {
					savePeriod(candelsBefore.get(0).getDate(), Iterables.getLast(candelsBefore).getDate());
				}
			}
			//			new Thread(() -> {
			//				List<Double> rsiValues = candelsBefore.stream()
			//						.map(c -> mapIndicator.get(timerange).get(c.getDate()))
			//						.filter(v -> v != null)
			//						.toList();
			//				//				plot(rsiValues, 0, 100);
			//			}).start();
			//			LocalDateTime lastDate = candelsBefore.get(candelsBefore.size()-1).getDate();
			//			System.out.println("last candel " + lastDate + " " + mapIndicator.get(timerange).get(lastDate));
			//			candels.forEach(c -> {
			//				content.getPoints().forEach(date -> {
			//					if (c.getDate().isEqual(date)) {
			//						points.add(c);
			//					}
			//				});
			//			});

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

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
		//		levelService.saveTimerange(timeRange);
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

		candels = candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateDesc(
				data.getMarket().getCode(),
				timeRange, 
				period.getDateStart(),
				period.getDateEnd(),
				PageRequest.of(0, MAX_CANDELS));
		for (int i = 0; i < 10 && candels.size() < 100; i++) {
			stretchZone(10);
			period = getPeriodOfTime();
			candels = candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateDesc(
					data.getMarket().getCode(),
					timeRange, 
					period.getDateStart(),
					period.getDateEnd(),
					PageRequest.of(0, MAX_CANDELS));
			System.out.println("Auto expand zone");
		}
		Collections.reverse(candels);

		//		topPanel.setNbCandels(candels.size());
		candels.stream().forEach(c -> c.setColor(null));
//		List<Accumulation> allAccumulations = accumulationRespository.findByMarketAndStatus(data.getMarket().getCode(), "OK");
		//		allAccumulations.forEach(acc -> {
		//			candels.stream().filter(c -> DateHelper.getTruncatedDate(c.getDate(), timeRange).equals(
		//					DateHelper.getTruncatedDate(acc.getDateEnd(), timeRange))).forEach(c -> c.setColor(Color.YELLOW));
		//		});
		points.forEach(candel -> {
			candels.stream().filter(c -> c.getDate().equals(candel.getDate())).forEach(c -> c.setColor(Color.WHITE));
		});
		if (acc != null && acc.getContent() != null) {
			acc.getContent().getPoints().forEach(date -> {
				candels.stream().filter(c -> c.getDate().equals(date)).forEach(c -> c.setColor(Color.CYAN));
			});
		}
		data.setCandels(candels);
		if (currentMarket == null || !currentMarket.equals(data.getMarketCode())) {
			currentMarket = data.getMarketCode();
			leftPanel.updateButtons();
		}
	}



	public void stretchZone(int value) {
		EnumTimeRange timeRange = data.getTimeRange();
		PeriodOfTime period = getPeriodOfTime();
		Pageable page = PageRequest.of(0, value);
		List<Candel> candels = candelRepository.findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(
				data.getMarket().getCode() ,timeRange, period.getDateStart(), page);
		if (candels != null && !candels.isEmpty()) {
			savePeriod(Iterables.getLast(candels).getDate(), period.getDateEnd());
		}
		mainPanel.unzoomMaximum();
	}

	private void shiftBackwardBy(int nbShift) {
		EnumTimeRange timeRange = data.getTimeRange();
		PeriodOfTime period = getPeriodOfTime();
		Pageable page = PageRequest.of(0, nbShift);
		List<Candel> nextCandels = candelRepository.findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(data.getMarket().getCode() ,timeRange, period.getDateEnd(), page);
		saveDateEnd(Iterables.getLast(nextCandels).getDate());
	}


	private void shiftForwardBy(int nbShift) {
		EnumTimeRange timeRange = data.getTimeRange();
		PeriodOfTime period = getPeriodOfTime();
		Pageable page = PageRequest.of(0, nbShift);
		List<Candel> nextCandels = candelRepository.findByMarketAndTimeRangeAndDateAfterOrderByDateAsc(data.getMarket().getCode(), timeRange, period.getDateEnd(), page);
		saveDateEnd(Iterables.getLast(nextCandels).getDate());
	}

	public List<Candel> getCandels() {
		return candels;
	}

	public void keyReleased(int keyCode) {
		enabledAccumulation = false;
		enabledZoom = false;
	}

	public void keyPressed(int keyCode) {
		if (!enabled) {
			return;
		}
		//ctrl
		if (keyCode == 17) {
			enabledAccumulation = true;
		}
		if (keyCode == 16) {
			enabledZoom = true;
			topPanel.getTimeRangeComboBox().setSelectedItem(candels);
		}

	}


	public void onClick(LocalDateTime date, Double price) {
		if (enabledAccumulation) {
			Candel candel = data.getMapCandels().get(date);
			points.add(candel);
			topPanel.setTradeInfo(points.size() + "/4", Color.GREEN);
			if (points.size() == 4) {
				Accumulation acc = new Accumulation();
				acc.setCreationDate(LocalDateTime.now());
				acc.setType("ACCUMULATION");
				acc.setTimeRange(data.getTimeRange());
				acc.setStatus("OK");
				acc.setMarket(data.getMarket().getCode());
				acc.setDateStart(points.get(0).getDate());
				acc.setDateEnd(points.get(points.size()-1).getDate());
				AccumulationContent content = new AccumulationContent(points.stream().map(c -> c.getDate()).toList());
				try {
					acc.setContentJson(objectMapper.writeValueAsString(content));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				accumulationRespository.save(acc);
				System.out.println("Accumulation created with end at " + points.get(points.size()-1).getDate());
				points.clear();
				topPanel.setTradeInfo("", Color.GREEN);
			}
			mainPanel.reload(false);
		} else if (enabledZoom) {
			EnumTimeRange timerange = EnumTimeRange.M1;
			data.setTimeRange(timerange);
			saveDateEnd(date);
			topPanel.getTimeRangeComboBox().setSelectedItem(timerange);
			mainPanel.reload(false);
		}
	}


	public void mouseMoved(LocalDateTime date, Double price) {
		Double rsi;
		if (mapIndicator.get(data.getTimeRange()) != null && (rsi = mapIndicator.get(data.getTimeRange()).get(date)) != null) {
			topPanel.updateIndicator(rsi);
		}
	}



}

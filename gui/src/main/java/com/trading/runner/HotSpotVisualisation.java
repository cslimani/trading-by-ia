package com.trading.runner;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JButton;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.trading.dto.HorizontalLine;
import com.trading.dto.jsonb.HotSpotData;
import com.trading.entity.Candle;
import com.trading.entity.HotSpot;
import com.trading.enums.EnumTimeRange;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Profile("hotspot-visu")
@Component
@Slf4j
public class HotSpotVisualisation extends AbstractRunner {

	private HotSpot hotspot;
	private int index = 0;
	private List<HotSpot> hotSpots;
	private JButton saveButton;

	@Override
	public void init() {
		super.init();
		addButton("Previous", () -> previous(), true);
		addButton("Next", () -> next(), true);
		//		addButton("Delete", () -> delete(),  Color.RED);
		saveButton = addButton("Create", () -> saveHotSpot(),  Color.GREEN);
		saveButton = addButton("Update", () -> update(),  Color.GREEN);
//		addButton("Delete", () -> delete(),  Color.RED);
		//		addLabel("BEARISH IMPULSE");
		//
		//		addSeparator(40);
		//		addLabel("BREAK & GO");
		//		addSeparator(40);
		//		addLabel("BREAK & RETURN");
		//		addLabel("BREAK & RANGE ON TOP");
		//		addLabel("BREAK & RANGE IN OUT");
		//		addLabel("BREAK & EXTEND");
		//
		//		addSeparator(40);
		//
		//		addLabel("PULL BACK IN > MID");
		//		addLabel("PULL BACK IN < MID");
		//
		//		addSeparator(40);
		//
		//		addLabel("SPRING");
		//		addLabel("INTERNAL SPRING");
		//
		//		addSeparator(40);
		//		addLabel("END GO UP");
		//		addLabel("END GO DOWN");

		
//		addLabel("TOUCH");
//		addLabel("BREAK < 1 ATR");
//		addLabel("BREAK < 20%");
//		addLabel("MEGA BREAK");
//
//		addSeparator(40);
//
//		addLabel("ONE BAR RETURN");
//		addLabel("QUICK RETURN");
//		addLabel("SLOW RETURN");
//
//		addSeparator(40);
//
//		addLabel("PULLBACK < RANGE");
//		addLabel("PULLBACK 5%");
//		addLabel("PULLBACK 10%");
//		addLabel("PULLBACK 15%");
//		addLabel("PULLBACK MIDDLE");
//
//		addSeparator(40);
//
//		addLabel("RANGE UNDER RANGE");
//		addLabel("RANGE ABOVE RANGE");
//
//		addSeparator(40);
//
//		addLabel("CHOCH");
//		addLabel("DOUBLE SPRING");
//
//		addSeparator(40);
//
//		addLabel("GO TO SPACE");
//		addLabel("GO BASEMENT");
		
		//Range
//		addLabelAndNext("SPRING FAIL", COLOR_RED);
//		addLabelAndNext("SPRING BE", "#FFCC87");
//		addLabelAndNext("SPRING WIN", "#66FF52");
//		
//		addSeparator(50);
//		
//		addLabelAndNext("DOUBLE SPRING FAIL", COLOR_RED);
//		addLabelAndNext("DOUBLE SPRING OK", "#66FF52");
//		
//		addSeparator(50);
//		
//		addLabelAndNext("INTERNAL SPRING FAIL", COLOR_RED);
//		addLabelAndNext("INTERNAL SPRING OK", "#66FF52");
//		
//		addSeparator(50);
//		addLabel("TOUCH AND GO UP");
//		
//		addSeparator(50);
//		addLabelAndNext("GO DOWN", COLOR_RED);
//		addLabelAndNext("INVALID RANGE", "#FFCC87");
//		addLabelAndNext("GO UP", "#66FF52");
		
		addLabelAndNext("TOUCH AND GO", "#66FF52");
		addLabelAndNext("SMALL WICK", "#66FF52");
		addLabelAndNext("NORMAL WICK", "#66FF52");
		addLabelAndNext("BIG WICK", "#66FF52");
		addLabelAndNext("MANY WICKS", "#66FF52");
		addLabelAndNext("MANY CANDLES", "#66FF52");
		addLabelAndNext("DEEP BREAK", "#66FF52");
		
	}

	@Transactional
	private void update() {
		hotspot.setDateEnd(Iterables.getLast(data.getCandles()).getDate());
		hotSpotRepository.save(hotspot);
	}

	private void addLabelAndNext(String label) {
		addLabelAndNext(label, null);		
	}

	private void addLabelAndNext(String label, String color) {
		addLabel(label, color, () -> next());
		saveButton.setEnabled(true);
	}

	private void saveHotSpot() {
		hotspot.getData().setLabels(labelList);
		HotSpot newHotspot = HotSpot.builder()
//				.code("RANGE_LABEL")
				.code("SPRING_LABEL")
				.market(hotspot.getMarket())
				.creationDate(LocalDateTime.now())
				.timeRange(data.getTimeRange())
				.dateStart(hotspot.getDateStart())
				.dateEnd(Iterables.getLast(data.getCandles()).getDate())
				.data(HotSpotData.builder().labels(labelList).build())
				.keyDates(hotspot.getKeyDates())
				.data(hotspot.getData())
				.build();
		hotSpotRepository.save(newHotspot);
//		hotSpotRepository.delete(hotspot);
		topPanel.setTradeInfo("OK", Color.GREEN);
		saveButton.setEnabled(false);
		clearLabels();
		next();
	}

	//	private void saveHotSpot() {
	//		LocalDateTime newDateStart = data.getCandles()
	//				.stream()
	//				.min(Comparator.comparingLong(c -> Duration.between(c.getDate(), hotspot.getDateStart()).abs().toMillis()))
	//				.get().getDate();
	//		
	//		HotSpot newHotspot = HotSpot.builder()
	//				.code("RANGE_TO_FIND")
	//				.market(hotspot.getMarket())
	//				.creationDate(LocalDateTime.now())
	//				.timeRange(data.getTimeRange())
	//				.dateStart(newDateStart)
	//				.dateEnd(Iterables.getLast(data.getCandles()).getDate())
	//				.build();
	//		hotSpotRepository.save(newHotspot);
	//		topPanel.setTradeInfo("OK", Color.GREEN);
	//	}

//	private void delete() {
//		hotSpotRepository.delete(hotspot);
//		log.info("HotSpot deleted");
//		next();
////		runnerInit();
//	}

	private void previous() {
		saveButton.setEnabled(true);
		topPanel.setTradeInfo("", Color.GREEN);
		if (index > 0) {
			index--;
			loadElement();
		}
	}

	private void next() {
		saveButton.setEnabled(true);
		topPanel.setTradeInfo("", Color.GREEN);
		if (index < hotSpots.size()-1) {
			if (!labelList.isEmpty()) {
				saveHotSpot();
			}
			index++;
			loadElement();
		}
	}

	@Override
	public void keyPressed(int keyCode) {
		super.keyPressed(keyCode);
		if (keyCode == 102) {
			next();
		} else if (keyCode == 100) {
			previous();
		}
		mainPanel.repaint();
		topPanel.repaint();
		mainPanel.reload();
	}
	
	@Override
	protected void runnerInit() {
		setup("EURUSD", EnumTimeRange.M15);
//		String type = "SPRING_LABEL";
//		String type = "RANGE_TO_AVOID";
//		String type = "RANGE_LABEL";
		String type = "WIN_RANGE";
//		String type = "LOST_RANGE";
//		String type = "DOWN_RANGE";
		
		
		hotSpots = hotSpotRepository.findByCodeAndMarketOrderByDateEnd(type, "GOLD");
		hotSpots = hotSpots.stream()
//				.filter(hs -> hs.getData().getLabels().contains("SPRING WIN"))
//				.filter(hs -> hs.getData().getLabels().contains("SPRING FAIL"))
//				.filter(hs -> hs.getData().getLabels() == null || hs.getData().getLabels().isEmpty())
				.toList();
		
		System.out.println(hotSpots.size() + " HotSpots found for type " + type);
		if (hotSpots.isEmpty()) {
			log.info("No HotSpot found for type " + type);
			System.exit(-1);
		}
		loadElement();
	}

	public void loadElement(){
		hotspot = hotSpots.get(index);
		topPanel.setDateAndPrice(hotspot.getDateEnd(), 0d);
		log.info("Loading HotSpot with id {}", hotspot.getId());
		dateStart = hotspot.getDateStart();
		dateEnd =  hotspot.getDateEnd();
		EnumTimeRange tr = hotspot.getTimeRange();
//		dateStart = LocalDateTime.of(2020, Month.JANUARY, 21, 9, 50);
//		dateEnd =  LocalDateTime.of(2020, Month.JANUARY, 21, 15, 20);
		
//		tr = EnumTimeRange.S30;
//		Double min = getMin(hotspot);
//		data.setPricesToDraw(List.of(new HorizontalLine(min, "#FFFFFF", "",dateStart)));
		
		data.setTimeRange(tr);
		topPanel.setTimeRange(tr);
		
		data.setMarketCode(hotspot.getMarket());
		HotSpotData hsData = hotspot.getData();
		data.setDatesToColor(Map.of(dateStart,"#FFFFFF", dateEnd,"#FFFFFF"));
		
		if (hsData != null) {
			data.setPricesToDraw(hsData.getLines());
			if (hsData.getPoints() != null) {
				data.setDatesToColor(hsData.getPoints().stream()
						.collect(Collectors.toMap(
								s -> s.getDate(),
								s -> s.getColor(),
								(c1, _) -> c1)));
			}
		}
		clearLabels();
	}


	private Double getMin(HotSpot hs) {
		List<Candle> candlesForMin = candleRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(hs.getMarket(), hs.getTimeRange(), 
				hs.getDateStart(), hs.getDateEnd());
		candlesForMin.removeLast();
		return candlesForMin.stream().mapToDouble(c -> c.getLow()).min().getAsDouble();
	}

	private Optional<Integer> getIndex(LocalDateTime date) {
		return Optional.ofNullable(data.getMapCandles().get(date)).map(c -> c.getIndex());
	}

	public void afterPricesLoaded(List<Candle> list) {
//		if (hotspot != null && hotspot.getKeyDates() != null) {
//			list.stream()
//			.filter(c -> hotspot.getKeyDates().contains(c.getDate()) )
//			.forEach(c -> c.setSelected(true));
//		}
		//		Optional<HotSpot> anyExists = hotSpotRepository.findByCodeAndMarketAndTimeRange("RANGE_TO_FIND", hotspot.getMarket(), hotspot.getTimeRange())
		//				.stream()
		//				.filter(hs -> getIndex(hs.getDateEnd()).isPresent() && getIndex(hotspot.getDateEnd()).isPresent())
		//				.filter(hs -> Math.abs(getIndex(hs.getDateEnd()).get() - getIndex(hotspot.getDateEnd()).get()) < 10)
		//				.findAny();
		//		if (anyExists.isPresent()) {
		//			topPanel.setTradeInfo("ALREADY FLAGED", Color.GREEN);
		//		}
//		HotSpot labeledHotSpot = hotSpotRepository.findByCodeAndMarketAndTimeRangeAndDateStart(
//				"RANGE_LABELED", hotspot.getMarket(),
//				hotspot.getTimeRange(), hotspot.getDateStart());
//		if (labeledHotSpot != null) {
////			topPanel.setTradeInfo("ALREADY FLAGED", Color.GREEN);
//			saveButton.setEnabled(false);
//		} else {
//			topPanel.setTradeInfo("", Color.WHITE);
//			saveButton.setEnabled(true);
//		}
	}


}

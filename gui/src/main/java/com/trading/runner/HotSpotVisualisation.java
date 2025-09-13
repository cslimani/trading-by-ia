package com.trading.runner;

import java.awt.Color;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JButton;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.trading.dto.jsonb.HotSpotData;
import com.trading.entity.Candle;
import com.trading.entity.HotSpot;
import com.trading.enums.EnumTimeRange;

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
		saveButton = addButton("Save", () -> saveHotSpot(),  Color.GREEN);

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



		addLabel("TOUCH");
		addLabel("BREAK < 1 ATR");
		addLabel("BREAK < 20%");
		addLabel("MEGA BREAK");

		addSeparator(40);

		addLabel("ONE BAR RETURN");
		addLabel("QUICK RETURN");
		addLabel("SLOW RETURN");

		addSeparator(40);

		addLabel("PULLBACK < RANGE");
		addLabel("PULLBACK 5%");
		addLabel("PULLBACK 10%");
		addLabel("PULLBACK 15%");
		addLabel("PULLBACK MIDDLE");

		addSeparator(40);

		addLabel("RANGE UNDER RANGE");
		addLabel("RANGE ABOVE RANGE");

		addSeparator(40);

		addLabel("CHOCH");
		addLabel("DOUBLE SPRING");

		addSeparator(40);

		addLabel("GO TO SPACE");
		addLabel("GO BASEMENT");
	}

	private void saveHotSpot() {
		HotSpot newHotspot = HotSpot.builder()
				.code("TRADE_LABELED")
				.market(hotspot.getMarket())
				.creationDate(LocalDateTime.now())
				.timeRange(data.getTimeRange())
				.dateStart(hotspot.getDateStart())
				.dateEnd(Iterables.getLast(data.getCandles()).getDate())
				.data(HotSpotData.builder().labels(labelList).build())
				.build();
		hotSpotRepository.save(newHotspot);
		//		hotSpotRepository.delete(hotspot);
		topPanel.setTradeInfo("OK", Color.GREEN);
		saveButton.setEnabled(false);
		clearLabels();
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

	private void delete() {
		hotSpotRepository.delete(hotspot);
		log.info("HotSpot deleted");
		previous();
		runnerInit();
	}

	private void previous() {
		topPanel.setTradeInfo("", Color.GREEN);
		if (index > 0) {
			index--;
			loadElement();
		}
	}

	private void next() {
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
	protected void runnerInit() {
		setup("EURUSD", EnumTimeRange.M15);
		String type = "TRADE";
		hotSpots = hotSpotRepository.findByCodeOrderByDateEnd(type);
		if (hotSpots.isEmpty()) {
			log.info("No HotSpot found for type " + type);
			System.exit(MAX_CANDLES);
		}
		loadElement();
	}

	public void loadElement(){
		hotspot = hotSpots.get(index);
		topPanel.setDateAndPrice(hotspot.getDateEnd(), 0d);
		log.info("Loading HotSpot with id {}", hotspot.getId());
		dateStart = hotspot.getDateStart();
		dateEnd =  hotspot.getDateEnd();
		data.setTimeRange(hotspot.getTimeRange());
		topPanel.setTimeRange(hotspot.getTimeRange());
		data.setMarketCode(hotspot.getMarket());
		HotSpotData hsData = hotspot.getData();
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


	private Optional<Integer> getIndex(LocalDateTime date) {
		return Optional.ofNullable(data.getMapCandles().get(date)).map(c -> c.getIndex());
	}

	public void afterPricesLoaded(List<Candle> list) {
		if (hotspot != null && hotspot.getKeyDates() != null) {
			list.stream()
			.filter(c -> hotspot.getKeyDates().contains(c.getDate()) )
			.forEach(c -> c.setSelected(true));
		}
		//		Optional<HotSpot> anyExists = hotSpotRepository.findByCodeAndMarketAndTimeRange("RANGE_TO_FIND", hotspot.getMarket(), hotspot.getTimeRange())
		//				.stream()
		//				.filter(hs -> getIndex(hs.getDateEnd()).isPresent() && getIndex(hotspot.getDateEnd()).isPresent())
		//				.filter(hs -> Math.abs(getIndex(hs.getDateEnd()).get() - getIndex(hotspot.getDateEnd()).get()) < 10)
		//				.findAny();
		//		if (anyExists.isPresent()) {
		//			topPanel.setTradeInfo("ALREADY FLAGED", Color.GREEN);
		//		}
		HotSpot labeledHotSpot = hotSpotRepository.findByCodeAndMarketAndTimeRangeAndDateStart(
				"RANGE_LABELED", hotspot.getMarket(),
				hotspot.getTimeRange(), hotspot.getDateStart());
		if (labeledHotSpot != null) {
//			topPanel.setTradeInfo("ALREADY FLAGED", Color.GREEN);
			saveButton.setEnabled(false);
		} else {
			topPanel.setTradeInfo("", Color.WHITE);
			saveButton.setEnabled(true);
		}
	}


}

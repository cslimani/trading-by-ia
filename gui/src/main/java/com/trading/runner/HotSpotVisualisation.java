package com.trading.runner;

import java.awt.Color;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
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

	@Override
	public void init() {
		super.init();
		addButton("Previous", () -> previous(), true);
		addButton("Next", () -> next(), true);
		addButton("Delete", () -> delete(),  Color.RED);
		addButton("Save", () -> saveHotSpot(),  Color.GREEN);
	}
	
	private void saveHotSpot() {
		LocalDateTime newDateStart = data.getCandles()
				.stream()
				.min(Comparator.comparingLong(c -> Duration.between(c.getDate(), hotspot.getDateStart()).abs().toMillis()))
				.get().getDate();
		
		HotSpot newHotspot = HotSpot.builder()
				.code("RANGE_TO_FIND")
				.market(hotspot.getMarket())
				.creationDate(LocalDateTime.now())
				.timeRange(data.getTimeRange())
				.dateStart(newDateStart)
				.dateEnd(Iterables.getLast(data.getCandles()).getDate())
				.build();
		hotSpotRepository.save(newHotspot);
		topPanel.setTradeInfo("OK", Color.GREEN);
	}

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
			index++;
			loadElement();
		}
	}

	@Override
	protected void runnerInit() {
		setup("EURUSD", EnumTimeRange.M15);
		String type = "RANGE_AUTO";
		hotSpots = hotSpotRepository.findByCodeOrderByDateEnd(type);
		if (hotSpots.isEmpty()) {
			log.info("No HotSpot found for type " + type);
			System.exit(MAX_CANDLES);
		}
		loadElement();
	}

	public void loadElement(){
		hotspot = hotSpots.get(index);
		
		log.info("Loading HotSpot with id {}", hotspot.getId());
		dateStart = hotspot.getDateStart();
		dateEnd =  hotspot.getDateEnd();
		data.setTimeRange(hotspot.getTimeRange());
		topPanel.setTimeRange(hotspot.getTimeRange());
		data.setMarketCode(hotspot.getMarket());
		if (hotspot.getData() != null) {
			data.setPricesToDraw(hotspot.getData().getLines());
		}
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
		Optional<HotSpot> anyExists = hotSpotRepository.findByCodeAndMarketAndTimeRange("RANGE_TO_FIND", hotspot.getMarket(), hotspot.getTimeRange())
				.stream()
				.filter(hs -> getIndex(hs.getDateEnd()).isPresent() && getIndex(hotspot.getDateEnd()).isPresent())
				.filter(hs -> Math.abs(getIndex(hs.getDateEnd()).get() - getIndex(hotspot.getDateEnd()).get()) < 10)
				.findAny();
		if (anyExists.isPresent()) {
			topPanel.setTradeInfo("ALREADY FLAGED", Color.GREEN);
		}
	}


}

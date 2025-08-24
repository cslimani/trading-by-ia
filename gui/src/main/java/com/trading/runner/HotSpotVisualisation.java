package com.trading.runner;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
		addButton("Delete", () -> delete(), true);
	}
	
	private void delete() {
		hotSpotRepository.delete(hotspot);
		log.info("HotSpot deleted");
		previous();
		runnerInit();
	}

	private void previous() {
		if (index > 0) {
			index--;
			loadElement();
		}
	}

	private void next() {
		if (index < hotSpots.size()-1) {
			index++;
			loadElement();
		}
	}

	@Override
	protected void runnerInit() {
		setup("US100.cash", EnumTimeRange.M15);
		String type = "ACCUMULATION_RECORDED";
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
	}

	protected void applyToCandles(List<Candle> list) {
		if (hotspot != null && hotspot.getKeyDates() != null) {
			list.stream()
			.filter(c -> hotspot.getKeyDates().contains(c.getDate()))
			.forEach(c -> c.setSelected(true));
		}
	}


}

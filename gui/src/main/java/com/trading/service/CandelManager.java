package com.trading.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.trading.dto.LightCandel;
import com.trading.entity.Candel;
import com.trading.entity.Market;
import com.trading.enums.EnumTimeRange;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;

@Service
public class CandelManager {

	Map<String, List<Candel>> mapMarketCandels = new ConcurrentHashMap<>();
	
	public List<Candel> findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(
			String market,
			EnumTimeRange timerange,
			LocalDateTime dateStart,
			LocalDateTime dateEnd) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Candel> findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(String code, EnumTimeRange timeRange,
			LocalDateTime localDateTime, PageRequest page) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Candel> findByMarketAndTimeRangeAndDateBetweenOrderByDateDesc(String code, EnumTimeRange timeRange,
			LocalDateTime dateStart, LocalDateTime dateEnd, PageRequest of) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Candel> findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(String code, EnumTimeRange timeRange,
			LocalDateTime dateStart, Pageable page) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Candel> findByMarketAndTimeRangeAndDateAfterOrderByDateAsc(String code, EnumTimeRange timeRange,
			LocalDateTime dateEnd, Pageable page) {
		// TODO Auto-generated method stub
		return null;
	}

	private List<Candel> getCandels(String market, LocalDateTime startDate, LocalDateTime endDate, EnumTimeRange timerange) {
		if (startDate.getYear() != endDate.getYear()) {
			throw new IllegalArgumentException("years are different");
		}
		int year = startDate.getYear();
		String id = buildId(market, year, timerange);
		List<Candel> candels = mapMarketCandels.get(id);
		if (candels == null) {
			String newCandelsFileName = String.format("/data/candels-queue/%s/%s/%d", market, timerange.name(), year);
			List<LightCandel> candelsLight = new ArrayList<>(1000000);
			try (ChronicleQueue queue = SingleChronicleQueueBuilder.single(newCandelsFileName).build()) {
				ExcerptTailer tailer = queue.createTailer();
				Wire wire = null;
				do { 
					try (final DocumentContext document = tailer.readingDocument()) {
						wire = document.wire();
						if (wire != null) {
							LightCandel cl = wire.bytes().readObject(LightCandel.class);
							candelsLight.add(cl);
						}
					}
				} while (wire != null);
			}
			candels = candelsLight.parallelStream().map(cl -> cl.toCandel()).toList();
			mapMarketCandels.put(id, candels);
		}
		return candels.parallelStream().filter(c -> c.getDate().isAfter(startDate) && c.getDate().isBefore(endDate)).toList();
	}

	private String buildId(String market, int year, EnumTimeRange timerange) {
		return market + "-" +year + "-" +timerange;
	}
}

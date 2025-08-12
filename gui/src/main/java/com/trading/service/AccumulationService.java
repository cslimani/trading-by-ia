package com.trading.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.AccumulationContent;
import com.trading.entity.Accumulation;
import com.trading.entity.AccumulationId;
import com.trading.entity.Candel;
import com.trading.enums.EnumTimeRange;
import com.trading.indicator.Indicator;
import com.trading.repository.AccumulationRespository;
import com.trading.repository.CandelRepository;
import com.trading.utils.AppUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AccumulationService {

	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	CandelRepository candelRepository;
	@Autowired
	AccumulationRespository accumulationRespository;

	public List<Accumulation> getAccumulationsFromIndicator(EnumTimeRange tr, String market, Indicator indicator) {
		List<Accumulation> accumulations = new ArrayList<Accumulation>();
		List<Candel> candelsIndicator = 
				candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(
						market, 
						tr,
						LocalDateTime.of(2015, 1, 1, 0, 0), 
						LocalDateTime.of(2016, 1, 1, 0, 0));
		IntStream.range(0, candelsIndicator.size()).boxed().forEach(i -> candelsIndicator.get(i).setIndex(i));

		candelsIndicator.stream()
		.filter(c -> indicator.get(c.getDate()) != null)
		.filter(c -> c.getIndex() > 500 && c.getIndex() < candelsIndicator.size() - 500)
		.filter(c -> indicator.get(c.getDate()) > 50)
		//		.filter(c -> matchPattern(c, candelsIndicator))
		//		.filter(c -> containsInAllAccumulations(c.getDate(), tr))
		.forEach(c -> {
			Double indicatorValue = indicator.get(c.getDate());
			Accumulation accumulation = findAccumulation(c, tr, candelsIndicator, market);
			if (accumulation == null) {
				return;
			}
			try {
				accumulation.setContentJson(objectMapper.writeValueAsString(accumulation.getContent()));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			accumulation.setIndicator(indicator.get(c.getDate()));
			Optional<Accumulation> match = accumulations.stream().filter(ac -> Math.abs(Duration.between(ac.getDateStart(),c.getDate()).toMinutes()) < 30).findFirst();
			if (match.isPresent()) {
				if (indicatorValue > match.get().getIndicator()) {
					accumulations.remove(match.get());
					accumulations.add(accumulation);
				} 
			} else {
				accumulations.add(accumulation);
			}
		});
		//				List<Double> rsiList = IntStream.range(0, candels.size()).boxed().map(i -> rsi.getRSI(i)).toList();

		//		return accumulations.stream().filter(a -> {
		//			return (exists(a, "ST").isEmpty() && exists(a, "FAILURE").isEmpty() && exists(a, "SPRING").isEmpty() && exists(a, "INVALID").isEmpty());
		//		}).toList();
		return accumulations;
	}


	private Optional<Accumulation> exists(Accumulation a, String type) {
		return accumulationRespository.findById(AccumulationId.builder()
				.dateStart(a.getDateStart())
				.market(a.getMarket())
				.type(type)
				.timeRange(a.getTimeRange()).build());
	}

	private Accumulation findAccumulation(Candel c, EnumTimeRange tr, List<Candel> candelsIndicator, String market) {
		int nbCandelAfterBreak = 5;
		Candel minBefore = AppUtils.getMin(c.getIndex()- 100, c.getIndex(), candelsIndicator);
		if (!minBefore.getDate().isEqual(c.getDate())) {
			return null;
		}

		Candel minAfter = AppUtils.getMin(c.getIndex(), c.getIndex() + 20, candelsIndicator);
		if (!minAfter.getDate().isEqual(c.getDate())) {
			return null;
		}
		Integer firstBreakIndex = null;
		for (int indexBreak = c.getIndex(); indexBreak < c.getIndex() + 200; indexBreak++) {
			Candel candelBreak = candelsIndicator.get(indexBreak);
			if (candelBreak.getLow() < c.getLow() && candelBreak.getHigh() > c.getLow()) {
				boolean ok = true;
				for (int p = indexBreak+1; p <= indexBreak + nbCandelAfterBreak; p++) {
					if (candelsIndicator.get(p).getLow() < c.getLow()) {
						ok = false;
						break;
					}
				}
				if (ok) {
					firstBreakIndex = indexBreak;
					break;
				}
			}
		}
		if (firstBreakIndex == null) {
			return null;
		}
		int countUnderMin = 0;
		for (int index = c.getIndex(); index < firstBreakIndex; index++) {
			if (candelsIndicator.get(index).getLow() < c.getLow()) {
				countUnderMin++;
			}
		}
		if (countUnderMin > 10) {
			return null;
		}

		Candel min = AppUtils.getMin(c.getIndex(), firstBreakIndex, candelsIndicator);
		Integer secondBreakIndex = null;
		for (int indexBreak = firstBreakIndex; indexBreak < firstBreakIndex + 200; indexBreak++) {
			Candel candelSecondBreak = candelsIndicator.get(indexBreak);
			if (candelSecondBreak.getLow() < min.getLow() && candelSecondBreak.getHigh() > min.getLow()) {
				boolean ok = true;
				for (int p = indexBreak+1; p <= indexBreak + nbCandelAfterBreak; p++) {
					if (candelsIndicator.get(p).getHigh() < min.getLow()) {
						ok = false;
						break;
					}
				}
				if (ok) {
					secondBreakIndex = indexBreak;
					break;
				}
			}
		}
		if (secondBreakIndex == null) {
			return null;
		}
		countUnderMin = 0;
		for (int index = min.getIndex(); index < secondBreakIndex; index++) {
			if (candelsIndicator.get(index).getLow() < min.getLow()) {
				countUnderMin++;
			}
		}
		if (countUnderMin > 10) {
			return null;
		}

		LocalDateTime endDate = candelsIndicator.get(secondBreakIndex+nbCandelAfterBreak).getDate();

		if (endDate.getHour() < 8 || endDate.getHour() > 20) {
			return null;
		}
		Accumulation accumulation = Accumulation.builder()
				.timeRange(tr)
				.dateStart(c.getDate())
				.dateEnd(endDate)
				.status("OK")
				.creationDate(LocalDateTime.now())
				.market(market)
				.content(new AccumulationContent(List.of(c.getDate(), c.getDate(), c.getDate(), endDate))).build();
		return accumulation;
	}


	private boolean matchPattern(Candel c, List<Candel> candelsIndicator) {
		Candel minBefore = AppUtils.getMin(c.getIndex()- 100, c.getIndex(), candelsIndicator);
		if (!minBefore.getDate().isEqual(c.getDate())) {
			return false;
		}

		Candel minAfter = AppUtils.getMin(c.getIndex(), c.getIndex() + 20, candelsIndicator);
		if (!minAfter.getDate().isEqual(c.getDate())) {
			return false;
		}
		Integer firstBreakIndex = null;
		for (int indexBreak = c.getIndex(); indexBreak < c.getIndex() + 200; indexBreak++) {
			Candel candelBreak = candelsIndicator.get(indexBreak);
			if (candelBreak.getLow() < c.getLow() && candelBreak.getHigh() > c.getLow()) {
				boolean ok = true;
				for (int p = indexBreak+1; p < indexBreak + 20; p++) {
					if (candelsIndicator.get(p).getLow() < c.getLow()) {
						ok = false;
						break;
					}
				}
				if (ok) {
					firstBreakIndex = indexBreak;
				}
			}
		}
		if (firstBreakIndex == null) {
			return false;
		}
		int countUnderMin = 0;
		for (int index = c.getIndex(); index < firstBreakIndex; index++) {
			if (candelsIndicator.get(index).getLow() < c.getLow()) {
				countUnderMin++;
			}
		}
		if (countUnderMin > 10) {
			return false;
		}

		//		Integer secondBreakIndex = null;
		//		for (int indexSecondBreak = firstBreakIndex; indexSecondBreak < firstBreakIndex + 200; indexSecondBreak++) {
		//			Candel candelFirstBreak = candelsIndicator.get(firstBreakIndex);
		//			Candel candelSecondBreak = candelsIndicator.get(indexSecondBreak);
		//			if (candelSecondBreak.getLow() < candelFirstBreak.getLow() && candelSecondBreak.getHigh() > candelFirstBreak.getLow()) {
		//				boolean ok = true;
		//				for (int p = indexSecondBreak+1; p < indexSecondBreak + 5; p++) {
		//					if (candelsIndicator.get(p).getClose() < candelFirstBreak.getLow()) {
		//						ok = false;
		//						break;
		//					}
		//				}
		//				if (!ok) {
		//					continue;
		//				}
		//				int countUnderFirstBreak = 0;
		//				for (int p = firstBreakIndex; p < indexSecondBreak ; p++) {
		//					if (candelsIndicator.get(p).getLow() < candelFirstBreak.getLow()) {
		//						countUnderFirstBreak++;
		//					}
		//				}
		//				if (countUnderFirstBreak < 10) {
		//					secondBreakIndex = indexSecondBreak;
		//					break;
		//				}
		//			}
		//		}
		//		if (secondBreakIndex == null) {
		//			return false;
		//		}
		//		System.out.println(countUnderMin);
		return true;
	}


	private boolean containsInAllAccumulations(LocalDateTime date, EnumTimeRange tr, List<Accumulation> allAccumulations) {
		return allAccumulations.stream()
				.filter(a -> a.getTimeRange() == tr)
				.anyMatch(a -> {
					try {
						a.setContent(objectMapper.readValue(a.getContentJson(), AccumulationContent.class));
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
					return Math.abs(Duration.between(a.getContent().getPoints().get(0), date).toMinutes()) < 30;
				});
	}


	public List<Accumulation> getAccumulationWithBOS(String market, EnumTimeRange tr, LocalDateTime dateStart, LocalDateTime dateEnd,
			Indicator indicator) {
		List<Accumulation> accumulations = new ArrayList<Accumulation>();
		List<Candel> candels = candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(
				market, 
				tr,
				dateStart, 
				dateEnd);
		IntStream.range(0, candels.size()).boxed().forEach(i -> candels.get(i).setIndex(i));
		for (int i = 310; i < candels.size() - 200; i++) {
			Candel c = candels.get(i);
			Candel min = AppUtils.getMin(c.getIndex() - 300, c.getIndex(), candels);
			if (!min.equals(c)) {
				continue;
			}
			for (int j = i+1; j < i+100; j++) {
				Candel cAfterMin = candels.get(j);
				if (cAfterMin.getLow() != null && cAfterMin.getLow() < min.getLow()) {
					break;
				}

				Candel maxBeforeMin = getLastMaxBefore(min.getIndex(), candels, 8);
				if (maxBeforeMin == null) {
					break;
				}
				//				if (maxBeforeMin.isDate(2023, 9, 4, 15, 5, 45)) {
				//					System.out.println();
				//				}

				Candel maxAfterMin = AppUtils.getMax(min.getIndex(), j, candels);
				double deltaUp = maxAfterMin.getHigh() - min.getLow();
				if (maxBeforeMin.getHigh() > maxAfterMin.getClose() - 0.05*deltaUp ) {
					continue;
				}

				double deltaDown = maxAfterMin.getHigh() - candels.get(j).getLow();
				double ratio = deltaDown/deltaUp;

				if (ratio < 1 && ratio > 0.5) {
					//					System.out.println();
					Accumulation acc = Accumulation.builder()
							.timeRange(tr)
							.market(market)
							.dateStart(c.getDate())
							.dateEnd(candels.get(cAfterMin.getIndex()+1).getDate())
							.build();
					accumulations.add(acc);
					break;
//					boolean found = false;
//					for (int k = 0; k < 300; k++) {
//						Candel candelLoop = candels.get(c.getIndex()-k);
//						if (indicator.get(candelLoop.getDate()) != null && indicator.get(candelLoop.getDate()) > 50) {
//							accumulations.add(acc);
//							found = true;
//							System.out.println("Acc at " + acc.getDateEnd() + " low date at " + candelLoop.getDate());
//							break;
//						}
//					}
//					if (found) {
//						break;
//					}
				}
			}
		}
		return accumulations;
	}


	private Candel getLastMaxBefore(Integer index, List<Candel> candels, int nbCandelsForMax) {
		for (int i = 0; i < 100; i++) {
			Candel c = candels.get(index - i);
			boolean isMax = true;
			for (int j = c.getIndex()-nbCandelsForMax; j < c.getIndex()+nbCandelsForMax; j++) {
				if (candels.get(j).getHigh() > c.getHigh()) {
					isMax = false;
					break;
				}
			}
			if (isMax) {
				return c;
			}
		}
		return null;
	}
}

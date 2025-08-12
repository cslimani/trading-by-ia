package com.trading.feature;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.trading.component.old.BosSelectedListener;
import com.trading.dto.PatternContent;
import com.trading.dto.PointOfPattern;
import com.trading.entity.TrainablePattern;
import com.trading.entity.TrainingTrade;
import com.trading.enums.EnumPatternType;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.Getter;

@Component
public class TrainablePatternComponent extends AbstractFeature{

	static final EnumPatternType PATTERN_TYPE = EnumPatternType.SUPPLY_ZONE;

	private boolean patternOnGoing;
	int sentiment = 0;
	List<BosSelectedListener> listeners = new ArrayList<>();
	List<PointOfPattern> pointsOfPattern = new ArrayList<>();
	@Getter
	TrainablePattern pattern;
	int index = 0;
	private List<TrainablePattern> patternsToProcess = List.of();
	private boolean multiplePatternsOnGoing = false;


	@PostConstruct
	public void init() {
		//		patternsToProcess = trainablePatternRepository.findByTypeAndSentimentIsNullOrderByPredictionDesc(PATTERN_TYPE);
		//				patternsToProcess = trainablePatternRepository.findByTypeAndSentiment(PATTERN_TYPE, 1);
		//				patternsToProcess = trainablePatternRepository.findByTypeAndSourceAndSentimentIsNullOrderByDateEnd(PATTERN_TYPE, "AUTO_FROM_MIN");
		//		patternsToProcess = trainablePatternRepository.findByTypeAndSourceAndSentimentOrderByDateEnd(PATTERN_TYPE, "AUTO_ENTRY_V1", 0);
		//		patternsToProcess = trainablePatternRepository.findByTypeAndSourceAndSentimentIsNullOrderByPredictionDesc(PATTERN_TYPE, "AUTO_FROM_MIN");
		//				patternsToProcess = trainablePatternRepository.findByTypeAndSourceOrderByDateEnd(PATTERN_TYPE, "TRADE_DOUBLE_LTF");
		//		patternsToProcess = trainablePatternRepository.findByTypeAndSourceOrderByDateEnd(PATTERN_TYPE, "ERROR_WICKOFF_V3");
		//		patternsToProcess = trainablePatternRepository.findByTypeAndSourceOrderByDateEnd(PATTERN_TYPE, "ANALYSIS_HTF");
		//		Iterator<TrainablePattern> it = patternsToProcess.iterator();

		//				patternsToProcess = trainablePatternRepository.findByTypeAndSourceOrderByDateEnd(PATTERN_TYPE, "AUTO_ENTRY_V1");

		//		TrainablePattern patternsManual = trainablePatternRepository.findFirstByTypeAndSourceOrderByDateEndDesc(PATTERN_TYPE, "MANUAL");

		//		while (it.hasNext()) {
		//			if (BooleanUtils.isTrue(it.next().getProcessed())) {
		//				it.remove();
		//			}
		//						if (it.next().getDateEnd().getYear() == 2015) {
		//							it.remove();
		//						}
		//						if (it.next().getTradeId() != null) {
		//							it.remove();
		//						}
		//		}
		//		patternsToProcess.forEach(pat -> {
		//			if (pat.getTradeId() != null) {
		//				TrainingTrade trade = trainingTradeRepository.findByIdentifier(pat.getTradeId());
		//				pat.setRiskRatio(trade.getRiskRatio());
		//			}
		//		});
		//		Collections.sort(patternsToProcess, (p1,p2) -> -p1.getRiskRatio().compareTo(p2.getRiskRatio()));
		System.out.println("Patterns to display " + patternsToProcess.size());
		loadData();
	}

	public void loadData() {
		if (patternsToProcess.size() > index) {
			pattern = patternsToProcess.get(index);
			if (pattern.getContentJson() != null) {
				try {
					pattern.setContent(objectMapper.readValue(pattern.getContentJson(), PatternContent.class));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void onClick(LocalDateTime date, Double price, int button) {
		if (patternOnGoing) {
			pointsOfPattern.add(new PointOfPattern(date, data.getTimeRange()));
		}
		if (patternOnGoing && pointsOfPattern.size() == 2) {
			savePattern();
			reset();
		}
	}

	public void savePattern() {
		TrainablePattern newPattern = new TrainablePattern();
		PatternContent content = pattern.getContent();
		content.setDateParentStart(pointsOfPattern.get(0).getDate());
		content.setDateEnterZone(pointsOfPattern.get(1).getDate());
		content.setDatesToColor(List.of());
		content.setPriceEnterZone(null);
		content.setTimerange(pointsOfPattern.get(0).getTimerange());
		try {
			newPattern.setContentJson(objectMapper.writeValueAsString(content));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		newPattern.setCreationDate(LocalDateTime.now());
		//		Candel candelEnd = data.getMapCandels().get(pointsOfPattern.get(1));
		//		newPattern.setDateEnd(data.getCandels().get(candelEnd.getIndex()+1).getDate());
		newPattern.setDateStart(pointsOfPattern.get(0).getDate());
		newPattern.setDateEnd(pointsOfPattern.get(1).getDate());
		newPattern.setSource("MANUAL_V3");
		newPattern.setMarket(data.getMarket().getCode());
		newPattern.setTimeRange(pointsOfPattern.get(1).getTimerange());
		newPattern.setType(EnumPatternType.SUPPLY_ZONE);
		trainablePatternRepository.save(newPattern);
		System.out.println("New pattern saved " + LocalDateTime.now());
	}

	public void keyPressed(int keyCode) {
		if (keyCode == 18) {
			patternOnGoing = true;
		}
		//		if (keyCode == 65406) {
		//			multiplePatternsOnGoing = true;
		//		}
		//		sentiment = 1;
		//		if (keyCode == 18) {
		//			sentiment = 0;
		//		}
		//				System.out.println(keyCode);
	}

	public void keyReleased(int keyCode) {
		reset();
	}

	public void addListener(BosSelectedListener listener) {
		listeners.add(listener);
	}

	public void reset() {
		pointsOfPattern.clear();
		patternOnGoing = false;
		multiplePatternsOnGoing = false;
	}

	public void next() {
		if (pattern != null) {
			pattern.setProcessed(true);
			pattern.setUpdateDate(LocalDateTime.now());
			trainablePatternRepository.save(pattern);
		}
		index++;
		loadData();
	}

	@Transactional
	public void delete() {
		trainablePatternRepository.delete(pattern);
		pattern = null;
	}

	public void setSentiment(int sentiment) {
		pattern.setSentiment(sentiment);
		pattern.setUpdateDate(LocalDateTime.now());
		trainablePatternRepository.save(pattern);
	}

	public void deleteLast() {
		TrainablePattern toDelete = trainablePatternRepository.findFirstByOrderByCreationDateDesc();
		trainablePatternRepository.delete(toDelete);
	}

	public void previous() {
		if (index > 0) {
			index--;
			loadData();
		}
	}

	@Transactional
	public void processed(Double rr) {
		pattern.setProcessed(true);
		pattern.setRiskRatio(rr);
		pattern.setUpdateDate(LocalDateTime.now());
		trainablePatternRepository.save(pattern);
	}

	@Transactional
	public void onCloseTrade(TrainingTrade trade) {
		//		TrainingTrade existingTrade = trainingTradeRepository.findByIdentifier(pattern.getTradeId());
		//		existingTrade.setDateOpen(trade.getDateOpen());
		//		trainingTradeRepository.save(existingTrade);
		if (pattern != null) {
//			trade.setDirection(pattern.getDirection());
			trainingTradeRepository.save(trade);
//			pattern.setTradeId(trade.getIdentifier());
			pattern.setProcessed(true);
			pattern.setRiskRatio(trade.getRiskRatio());
			pattern.setUpdateDate(LocalDateTime.now());
			trainablePatternRepository.save(pattern);
		}
	}

}

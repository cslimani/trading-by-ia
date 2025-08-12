package com.trading.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.trading.entity.TrainablePattern;
import com.trading.entity.TrainablePatternId;
import com.trading.enums.EnumPatternType;

public interface TrainablePatternRepository extends CrudRepository<TrainablePattern, TrainablePatternId>{

	List<TrainablePattern> findByTypeAndSentimentIsNullOrderByPredictionDesc(EnumPatternType patternType);

	TrainablePattern findFirstByOrderByCreationDateDesc();

	List<TrainablePattern> findByTypeAndSentiment(EnumPatternType patternType, int sentiment);

	List<TrainablePattern> findByTypeAndSource(EnumPatternType patternType, String source);

	List<TrainablePattern> findByTypeAndSourceAndSentimentIsNull(EnumPatternType patternType, String source);

	List<TrainablePattern> findByTypeAndSourceAndSentimentIsNullOrderByPredictionDesc(EnumPatternType patternType, String source);

	List<TrainablePattern> findByTypeAndSourceAndSentimentIsNullOrderByDateEnd(EnumPatternType patternType,	String source);

	List<TrainablePattern> findByTypeAndSourceOrderByDateEnd(EnumPatternType patternType, String source);

	List<TrainablePattern> findByTypeAndSourceAndSentimentOrderByDateEnd(EnumPatternType patternType, String source, int sentiment);

	List<TrainablePattern> findByTypeAndSourceOrderByPredictionDesc(EnumPatternType patternType, String source);

	List<TrainablePattern> findByTypeAndSourceAndSentimentOrderByDateEndDesc(EnumPatternType patternType, String string, int sentiment);

	TrainablePattern findFirstByTypeAndSourceOrderByDateEndDesc(EnumPatternType patternType, String string);


}

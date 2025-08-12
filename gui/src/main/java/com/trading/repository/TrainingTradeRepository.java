package com.trading.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.trading.entity.TrainingTrade;

public interface TrainingTradeRepository extends CrudRepository<TrainingTrade, Long>{


	List<TrainingTrade>  findByDateCloseIsNullOrderByDateOpen();

//	List<TrainingTrade>  findAllByOrderByPredictionDesc();

	TrainingTrade findFirstByOrderByDateCreationDesc();
	
	TrainingTrade findById(String tradeId);

	Optional<TrainingTrade> findFirstByMarketOrderByDateCloseDesc(String market);

	List<TrainingTrade> findByMarket(String market);

}

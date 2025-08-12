package com.trading.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.trading.entity.TickPrice;

public interface TickPriceRepository extends CrudRepository<TickPrice, UUID>{

	List<TickPrice> findByMarketAndDateBetweenOrderByDate(String market, LocalDateTime startDate, LocalDateTime endDate);

	List<TickPrice> findByMarketAndDateAfterOrderByDate(String market, LocalDateTime date);
	
	TickPrice findFirstByMarketOrderByDateDesc(String marketCode);


}
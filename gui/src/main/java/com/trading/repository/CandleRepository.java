package com.trading.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import com.trading.entity.Candle;
import com.trading.enums.EnumTimeRange;

public interface CandleRepository extends CrudRepository<Candle, Long>{

//	List<Candle> findByMarketAndDateBetweenOrderByDateAsc(String market, LocalDateTime dateStart, LocalDateTime dateEnd);

	List<Candle> findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(String market, EnumTimeRange timeRange, LocalDateTime dateStart, LocalDateTime dateEnd);
	
	List<Candle> findByMarketAndTimeRangeAndDateBetweenOrderByDateDesc(String market, 
			EnumTimeRange timeRange, LocalDateTime dateStart, LocalDateTime dateEnd, Pageable Pageable);
	
	List<Candle> findByMarketAndTimeRangeAndDateAfterOrderByDateAsc(String market, EnumTimeRange timeRange, LocalDateTime dateStart, Pageable pageable);

	List<Candle> findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(String market, EnumTimeRange timeRange, LocalDateTime dateEnd, Pageable page);

//	List<Candle> findFirstByMarketAndAndDateAfterOrderByDateAsc(String market, EnumTimeRange timeRange, LocalDateTime dateEnd, Pageable page);
	
	
}



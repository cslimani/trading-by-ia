package com.trading.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import com.trading.entity.Candel;
import com.trading.entity.CandelId;
import com.trading.enums.EnumTimeRange;

public interface CandelRepository extends CrudRepository<Candel, CandelId>{

//	List<Candel> findByMarketAndDateBetweenOrderByDateAsc(String market, LocalDateTime dateStart, LocalDateTime dateEnd);

	List<Candel> findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(String market, EnumTimeRange timeRange, LocalDateTime dateStart, LocalDateTime dateEnd);
	
	List<Candel> findByMarketAndTimeRangeAndDateBetweenOrderByDateDesc(String market, 
			EnumTimeRange timeRange, LocalDateTime dateStart, LocalDateTime dateEnd, Pageable Pageable);
	
	List<Candel> findByMarketAndTimeRangeAndDateAfterOrderByDateAsc(String market, EnumTimeRange timeRange, LocalDateTime dateStart, Pageable pageable);

	List<Candel> findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(String market, EnumTimeRange timeRange, LocalDateTime dateEnd, Pageable page);

//	List<Candel> findFirstByMarketAndAndDateAfterOrderByDateAsc(String market, EnumTimeRange timeRange, LocalDateTime dateEnd, Pageable page);
	
	
}



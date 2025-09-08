package com.trading.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trading.entity.HotSpot;
import com.trading.enums.EnumTimeRange;

public interface HotSpotRepository extends JpaRepository<HotSpot, String> {

	List<HotSpot> findByCode(String code);

	List<HotSpot> findByCodeOrderByDateEndDesc(String string);
	
	List<HotSpot> findByCodeOrderByDateEndAsc(String string);

	List<HotSpot> findByCodeOrderByDateEnd(String string);

	HotSpot findByCodeAndMarketAndTimeRangeAndDateStart(String code, String market, EnumTimeRange timeRange, LocalDateTime dateStart);

}

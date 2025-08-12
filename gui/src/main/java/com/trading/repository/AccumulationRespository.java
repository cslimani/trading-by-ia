package com.trading.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.trading.entity.Accumulation;
import com.trading.entity.AccumulationId;

public interface AccumulationRespository extends CrudRepository<Accumulation, AccumulationId>{

	List<Accumulation> findByMarketAndStatus(String code, String string);

	Accumulation findFirstByMarketAndStatusOrderByDateEndDesc(String code, String string);

	List<Accumulation> findByMarketAndStatusAndTypeOrderByDateEndAsc(String code, String status, String type);

	List<Accumulation> findByStatusAndDateEndAfter(String string, LocalDateTime minusMinutes);
	
}



package com.trading.repository;

import java.time.LocalDateTime;

import org.springframework.data.repository.CrudRepository;

import com.trading.entity.MomentOfInterest;
import com.trading.entity.MomentOfInterestId;

public interface MomentOfInterestRespository extends CrudRepository<MomentOfInterest, MomentOfInterestId>{


	MomentOfInterest findFirstByMarketOrderByDateEndDesc(String code);

	MomentOfInterest findFirstByMarketAndDateEndBeforeOrderByDateEndDesc(String code, LocalDateTime dateEnd);


	MomentOfInterest findFirstByMarketAndDateEndAfterOrderByDateEndAsc(String code, LocalDateTime dateEnd);


}

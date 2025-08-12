package com.trading.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.trading.entity.PointOfInterest;
import com.trading.entity.PointOfInterestId;

public interface PointOfInterestRespository extends CrudRepository<PointOfInterest, PointOfInterestId>{

	List<PointOfInterest> findByDone(boolean done);


}

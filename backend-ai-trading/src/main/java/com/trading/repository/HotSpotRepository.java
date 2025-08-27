package com.trading.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trading.entity.HotSpot;

public interface HotSpotRepository extends JpaRepository<HotSpot, String> {

	List<HotSpot> findByCode(String code);

	List<HotSpot> findByCodeOrderByDateEndDesc(String string);

	List<HotSpot> findByCodeOrderByDateEnd(String string);
}

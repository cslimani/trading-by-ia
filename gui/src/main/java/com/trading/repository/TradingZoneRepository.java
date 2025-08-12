package com.trading.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trading.entity.TradingZone;

@Repository
public interface TradingZoneRepository extends JpaRepository<TradingZone, Long> {

	List<TradingZone> findByDateStartBetween(LocalDateTime dateStart, LocalDateTime dateEnd);

	List<TradingZone> findByDateEndBetween(LocalDateTime dateStart, LocalDateTime dateEnd);

}

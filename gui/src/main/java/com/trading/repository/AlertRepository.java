package com.trading.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trading.entity.Alert;

public interface AlertRepository extends JpaRepository<Alert, UUID>{

	List<Alert> findByMarket(String marketCode);

	List<Alert> findBySent(boolean sent);

}

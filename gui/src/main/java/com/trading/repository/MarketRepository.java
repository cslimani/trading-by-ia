package com.trading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trading.entity.Market;

@Repository
public interface MarketRepository extends JpaRepository<Market, Long> {

	Market getByCode(String code);

}

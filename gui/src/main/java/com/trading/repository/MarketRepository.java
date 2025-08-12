package com.trading.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trading.entity.Market;

@Repository
public interface MarketRepository extends JpaRepository<Market, Long> {

	List<Market> findByEnabled(boolean b);

	Market getByCode(String code);

}

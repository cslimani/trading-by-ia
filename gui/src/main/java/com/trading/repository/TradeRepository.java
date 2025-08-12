package com.trading.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.trading.entity.Trade;
import com.trading.enums.EnumTradeStatus;

public interface TradeRepository extends CrudRepository<Trade, Long>{

	List<Trade> findByMarketAndStatus(String marketCode, EnumTradeStatus status);

	List<Trade> findByStatus(EnumTradeStatus open);

	
}

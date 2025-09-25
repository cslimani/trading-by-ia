package com.trading.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.trading.entity.Candle;
import com.trading.enums.EnumTimeRange;

public interface CandleRepository extends JpaRepository<Candle, String> {

	List<Candle> findByMarketAndTimeRangeAndDateBetweenOrderByDate(String market, EnumTimeRange tr,
			LocalDateTime startDate, LocalDateTime endDate);

	@Query(
        value = "SELECT * FROM candle c " +
                "WHERE c.market = :market " +
                "AND c.time_range = :timeRange " +
                "AND c.date > :date " +
                "ORDER BY c.date ASC " +
                "LIMIT 1 OFFSET :nbCandle",
        nativeQuery = true
    )
    Candle findNthCandleAfterDate(
        @Param("market") String market,
        @Param("timeRange") String timeRange,
        @Param("date") LocalDateTime date,
        @Param("nbCandle") int nbCandle
    );
	
	@Query(value = "SELECT * FROM candle c " +
			"WHERE c.market = :market " +
			"AND c.time_range = :timeRange " +
			"AND c.date < :date " +
			"ORDER BY c.date DESC " +
			"LIMIT 1 OFFSET :nbCandle",
			nativeQuery = true)
	Candle findNthCandleBeforeDate(@Param("market") String market, @Param("timeRange") String timeRange,
			@Param("date") LocalDateTime date, @Param("nbCandle") int nbCandle);
}

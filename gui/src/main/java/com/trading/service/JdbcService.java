package com.trading.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Component;

import com.trading.entity.Market;
import com.trading.entity.TickPrice;
import com.trading.enums.EnumTimeRange;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.annotation.PostConstruct;

@Component
public class JdbcService {

//	private JdbcTemplate jdbcTemplateRemote;
//	@Autowired
//	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	private DataSource remoteDataSource;


//	@PostConstruct
	public void init() {
//		jdbcTemplate = new JdbcTemplate(dataSource);
//		jdbcTemplate.setFetchSize(1000);

		HikariConfig hikariConfig = new HikariConfig();
		//		    hikariConfig.setDriverClassName("com.mysql.jdbc.Driver");
		hikariConfig.setJdbcUrl("jdbc:postgresql://trd:5432/meta_trading"); 
		hikariConfig.setUsername("meta_trading_usr");
		hikariConfig.setPassword("r2oi5_qzouif9-nqozi");
		hikariConfig.setMaximumPoolSize(5);
		hikariConfig.setConnectionTestQuery("SELECT 1");
		hikariConfig.setPoolName("RemoteHikariCP");
		remoteDataSource = new HikariDataSource(hikariConfig);
		jdbcTemplate = new JdbcTemplate(remoteDataSource);
		jdbcTemplate.setFetchSize(1000);
	}

	//	public List<Candel> getCandels(Market market, LocalDateTime dateStart, LocalDateTime dateEnd) {
	//		List<Candel> candels = cache.get(dateStart);
	//		Object[] args = { new SqlParameterValue(Types.BIGINT, market.getId()),
	//				new SqlParameterValue(Types.TIMESTAMP, dateStart), new SqlParameterValue(Types.TIMESTAMP, dateEnd) };
	//		String sql = "select id, low, high, open, close, date, spread from CANDEL where market_id=? AND date >= ? AND date < ? order by date";
	//		candels = jdbcTemplate.query(sql, new RowMapper<Candel>() {
	//			@Override
	//			public Candel mapRow(ResultSet rs, int rowNum) throws SQLException {
	//				Candel candel = new Candel();
	//				candel.setId(rs.getLong("id"));
	//				candel.setLow(rs.getFloat("low"));
	//				candel.setHigh(rs.getFloat("high"));
	//				candel.setOpen(rs.getFloat("open"));
	//				candel.setClose(rs.getFloat("close"));
	//				candel.setSpread(rs.getFloat("spread"));
	//				candel.setDate(rs.getTimestamp("date").toLocalDateTime());
	//				return candel;
	//			}
	//		}, args);
	//		return candels;
	//	}


//	public List<TickPrice > getTickPrice(Market market, LocalDateTime dateStart, LocalDateTime dateEnd, JdbcTemplate jdbcTemplate) {
//		Object[] args = { new SqlParameterValue(Types.BIGINT, market.getId()),
//				new SqlParameterValue(Types.TIMESTAMP, dateStart), new SqlParameterValue(Types.TIMESTAMP, dateEnd) };
//		String sql = "select id, ask, bid, date from tick_price where market=? and date >= ? and date < ? order by date";
//		return jdbcTemplate.query(sql, new RowMapper<TickPrice>() {
//			@Override
//			public TickPrice mapRow(ResultSet rs, int rowNum) throws SQLException {
//				TickPrice tickPrice = new TickPrice();
//				tickPrice.setAsk(rs.getDouble("ask"));
//				tickPrice.setDate(rs.getTimestamp("date").toLocalDateTime());
//				tickPrice.setBid(rs.getDouble("bid"));
//				return tickPrice;
//			}
//		}, args);
//	}

	public List<TickPrice> getTickPricesAfter(String marketCode, LocalDateTime date) {
		Object[] args = { new SqlParameterValue(Types.VARCHAR, marketCode),
				new SqlParameterValue(Types.TIMESTAMP, date) };
		String sql = "select id, date, ask, bid, market from tick_price where market=? and date > ?  order by date asc";
		return mapTickPrice(sql, args);
	}

	
//	public List<TickPrice> getTickPriceBeforeDate(Market market, LocalDateTime dateStart, LocalDateTime dateEnd, int limit) {
//		Object[] args = { new SqlParameterValue(Types.BIGINT, market.getId()),
//				new SqlParameterValue(Types.TIMESTAMP, dateStart),
//				new SqlParameterValue(Types.TIMESTAMP, dateEnd),
//				new SqlParameterValue(Types.INTEGER, limit) };
//		String sql = "select ask, bid, price, spread, date from tick_price where market_id=? and date >= ? and date <= ?  order by date desc limit ?";
//		return mapTickPrice(sql, args);
//	}

	private List<TickPrice> mapTickPrice(String sql, Object[] args) {
		return jdbcTemplate.query(sql, new RowMapper<TickPrice>() {
			@Override
			public TickPrice mapRow(ResultSet rs, int rowNum) throws SQLException {
				TickPrice tickPrice = new TickPrice();
				if (rs.getDouble("ask") > 0) {
					tickPrice.setAsk(rs.getDouble("ask"));
				}
				if (rs.getDouble("bid") > 0) {
					tickPrice.setBid(rs.getDouble("bid"));
				}
				tickPrice.setDate(rs.getTimestamp("date").toLocalDateTime());
				tickPrice.setId(rs.getString("id"));
				tickPrice.setMarket(rs.getString("market"));
				return tickPrice;
			}
		}, args);
	}

//	public List<TickPrice> getTickPriceAfterDate(Market market, LocalDateTime dateStart, LocalDateTime dateEnd,	int limit) {
//		Object[] args = { new SqlParameterValue(Types.BIGINT, market.getId()),
//				new SqlParameterValue(Types.TIMESTAMP, dateStart),
//				new SqlParameterValue(Types.TIMESTAMP, dateEnd),
//				new SqlParameterValue(Types.INTEGER, limit) };
//		String sql = "select ask, bid, price, spread, date from tick_price where market_id=? and date >= ? and date <= ?  order by date limit ?";
//		return mapTickPrice(sql, args);
//	}

//	public List<TickPrice > getTickPrice(Market market, LocalDateTime dateStart, LocalDateTime dateEnd) {
//		return this.getTickPrice(market, dateStart, dateEnd, jdbcTemplate); 
//	}


	//	public Candel getCandelBySQL(String sql,Market market, LocalDateTime start, LocalDateTime end) {
	//		List<Candel> list = getCandelBySQL(sql, market, start, end, jdbcTemplate);
	//		if (!list.isEmpty()) {
	//			return list.get(0);
	//		}
	//		return null;
	//	}

	//	public List<Candel> getCandelBySQL(String sql,Market market, LocalDateTime start, LocalDateTime end, JdbcTemplate jdbcTemplate) {
	//		Object[] args = { 
	//				new SqlParameterValue(Types.BIGINT, market.getId()), 
	//				new SqlParameterValue(Types.TIMESTAMP,start), 
	//				new SqlParameterValue(Types.TIMESTAMP, end)
	//		};
	//		return jdbcTemplate.query(sql, new RowMapper<Candel>() {
	//			@Override
	//			public Candel mapRow(ResultSet rs, int rowNum) throws SQLException {
	//				Candel candel = new Candel();
	//				candel.setId(rs.getLong("id"));
	//				candel.setHigh(rs.getFloat("high"));
	//				candel.setLow(rs.getFloat("low"));
	//				candel.setOpen(rs.getFloat("open"));
	//				candel.setClose(rs.getFloat("close"));
	//				candel.setSpread(rs.getFloat("spread"));
	////				candel.setMarket(market);
	//				candel.setDate(rs.getTimestamp("date").toLocalDateTime());
	//				return candel;
	//			}
	//		}, args);
	//	}

	//	public Candel findFirstByMarketAndDateBetweenOrderByHighDesc(Market market, LocalDateTime start, LocalDateTime end) {
	//		String sql = "select * from CANDEL where MARKET_ID=? and date >= ? and  date < ? order by high desc limit 1";
	//		return getCandelBySQL(sql, market, start, end);
	//	}
	//
	//	public Candel findFirstByMarketAndDateBetweenOrderByLowAsc(Market market, LocalDateTime start,	LocalDateTime end) {
	//		String sql = "select * from CANDEL where MARKET_ID=? and date >= ? and  date < ? order by low asc limit 1";
	//		return getCandelBySQL(sql, market, start, end);
	//	}
	//
	//	public Candel findFirstByMarketAndDateBetweenOrderByDateDesc(Market market, LocalDateTime start, LocalDateTime end) {
	//		String sql = "select * from CANDEL where MARKET_ID=? and date >= ? and  date < ? order by date desc limit 1";
	//		return getCandelBySQL(sql, market, start, end);
	//	}


//	public Double getIndicator(String name, String params, Market market, LocalDateTime date, EnumTimeRange timeRange) {
//		Object[] args = { new SqlParameterValue(Types.BIGINT, market.getId()),
//				new SqlParameterValue(Types.VARCHAR, name),
//				new SqlParameterValue(Types.VARCHAR, params),
//				new SqlParameterValue(Types.TIMESTAMP, date),
//				new SqlParameterValue(Types.VARCHAR, timeRange.toString()) };
//		String sql = "select value from indicator where market_id=? and name=? and params=? and date=? and time_range=?";
//		try {
//			return jdbcTemplate.queryForObject(sql, Double.class, args);
//		} catch (EmptyResultDataAccessException e) {
//			return null;
//		}
//	}

	//	public List<Candel> getCandelsBeforeDateOrderByDateDesc(Market market, int nbCandels, LocalDateTime date) {
	//		Object[] args = { new SqlParameterValue(Types.BIGINT, market.getId()),
	//				new SqlParameterValue(Types.TIMESTAMP, date),
	//				new SqlParameterValue(Types.BIGINT, nbCandels)};
	//		String sql = "select id, low, high, open, close, date, spread from CANDEL where market_id=? AND date <= ? order by date desc limit ?";
	//		List<Candel> candels  = jdbcTemplate.query(sql, new RowMapper<Candel>() {
	//			@Override
	//			public Candel mapRow(ResultSet rs, int rowNum) throws SQLException {
	//				Candel candel = new Candel();
	//				candel.setId(rs.getLong("id"));
	//				candel.setLow(rs.getFloat("low"));
	//				candel.setHigh(rs.getFloat("high"));
	//				candel.setOpen(rs.getFloat("open"));
	//				candel.setClose(rs.getFloat("close"));
	//				candel.setSpread(rs.getFloat("spread"));
	//				candel.setDate(rs.getTimestamp("date").toLocalDateTime());
	//				return candel;
	//			}
	//		}, args);
	//		return candels;
	//	}

	//	public List<Candel> getCandelsAfterDateOrderByDateAsc(Market market, int nbCandels, LocalDateTime date) {
	//		Object[] args = { new SqlParameterValue(Types.BIGINT, market.getId()),
	//				new SqlParameterValue(Types.TIMESTAMP, date),
	//				new SqlParameterValue(Types.BIGINT, nbCandels)};
	//		String sql = "select id, low, high, open, close, date, spread from CANDEL where market_id=? AND date > ? order by date limit ?";
	//		List<Candel> candels  = jdbcTemplate.query(sql, new RowMapper<Candel>() {
	//			@Override
	//			public Candel mapRow(ResultSet rs, int rowNum) throws SQLException {
	//				Candel candel = new Candel();
	//				candel.setId(rs.getLong("id"));
	//				candel.setLow(rs.getFloat("low"));
	//				candel.setHigh(rs.getFloat("high"));
	//				candel.setOpen(rs.getFloat("open"));
	//				candel.setClose(rs.getFloat("close"));
	//				candel.setSpread(rs.getFloat("spread"));
	//				candel.setDate(rs.getTimestamp("date").toLocalDateTime());
	//				return candel;
	//			}
	//		}, args);
	//		return candels;
	//	}

	//	public List<Candel> getCandelsBetweenDateOrderByDateDesc(Market market, LocalDateTime startDate, LocalDateTime endDate) {
	//		Object[] args = { new SqlParameterValue(Types.BIGINT, market.getId()),
	//				new SqlParameterValue(Types.TIMESTAMP, startDate),
	//				new SqlParameterValue(Types.TIMESTAMP, endDate)};
	//		String sql = "select id, low, high, open, close, date, spread from CANDEL where market_id=? AND date > ? AND date < ? order by date desc";
	//		List<Candel> candels  = jdbcTemplate.query(sql, new RowMapper<Candel>() {
	//			@Override
	//			public Candel mapRow(ResultSet rs, int rowNum) throws SQLException {
	//				Candel candel = new Candel();
	//				candel.setId(rs.getLong("id"));
	//				candel.setLow(rs.getFloat("low"));
	//				candel.setHigh(rs.getFloat("high"));
	//				candel.setOpen(rs.getFloat("open"));
	//				candel.setClose(rs.getFloat("close"));
	//				candel.setSpread(rs.getFloat("spread"));
	//				candel.setDate(rs.getTimestamp("date").toLocalDateTime());
	//				return candel;
	//			}
	//		}, args);
	//		return candels;
	//	}



}

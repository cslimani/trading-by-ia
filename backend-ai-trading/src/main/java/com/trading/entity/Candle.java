package com.trading.entity;

import java.time.LocalDateTime;

import com.trading.enums.EnumTimeRange;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "candle")
public class Candle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	public LocalDateTime date;
	public double open;
	public double high;
	public double low;
	public double close;
	@Enumerated(EnumType.STRING)
	EnumTimeRange timeRange;

	String market;
	
	@Transient
	public double tr;
	@Transient
	public double atr;
	@Transient
	public double atr_ratio;
	@Transient
	public Integer index;
	@Transient
	public int priorHigherLowRun; 
	@Transient
	public int posteriorHigherLowRun; 
	@Transient
	public Double ema;
	@Transient
	public Double emaSlope; 
	@Transient
	public Double rsi; 
	@Transient
	public Double macd;
	@Transient
	public Double macdSignal;
	
	@Transient
	public Double getMacdDiff() {
		if (macd != null && macdSignal != null) {
			return macd - macdSignal;
		}
		return null;
	}

	@Transient
	public double getMax() {
		return high;
	}
	@Transient
	public double getMin() {
		return low;
	}

	public boolean isTime(int hour, int minute, int sec) {
		return date.getMinute() == minute && date.getHour() == hour && date.getSecond() == sec;
	}

	public boolean isDate(int day, int month) {
		return date.getDayOfMonth() == day && date.getMonthValue() == month;
	}
	
	public boolean isBefore(Candle candle) {
		return date.isBefore(candle.getDate());
	}

	public boolean isAfter(Candle candle) {
		return date.isAfter(candle.getDate());
	}

	public boolean isEquals(Candle candle) {
		return date.isEqual(candle.getDate());
	}

}
package com.trading.entity;

import java.awt.Color;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.trading.enums.EnumTimeRange;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Entity
@Table
public class Candle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private LocalDateTime date;
	private String market;
	@Enumerated(EnumType.STRING)
	private EnumTimeRange timeRange;
	@Column(name = "low")
	private Double lowAsk;
	@Column(name = "high")
	private Double highAsk;
	@Column(name = "open")
	private Double openAsk;
	@Column(name = "close")
	private Double closeAsk;
	@Column(name = "low", insertable=false, updatable=false)
	private Double lowBid;
	@Column(name = "high", insertable=false, updatable=false)
	private Double highBid;
	@Column(name = "open", insertable=false, updatable=false)
	private Double openBid;
	@Column(name = "close", insertable=false, updatable=false)
	private Double closeBid;
	private Double volume;
	@Transient
	Color color;
	@Transient
	private Integer index;
	@Transient
	private boolean draw;
	@Transient
	private LocalDateTime endDate;
	@Transient
	boolean endTrade;
	@Transient
	boolean selected;

	public boolean isGoingUp() {
		return getClose() >= getOpen();
	}

	public boolean isGoingDown() {
		return getClose() <= getOpen();
	}

	public boolean isBefore(Candle candle) {
		return date.isBefore(candle.getDate());
	}
	
	public boolean isAfter(Candle candle) {
		return date.isAfter(candle.getDate());
	}
	
	public Double getMax() {
		return Math.max(getClose(), getOpen());
	}
	
	public Double getMin() {
		return Math.min(getClose(), getOpen());
	}

	public boolean isDate(int year, int month, int day, int hour, int min, int sec) {
		return date.isEqual(LocalDateTime.of(year, month, day, hour, min, sec));
	}

	public Double getOpen() {
		return openBid == null ? openAsk : openBid;
	}

	public Double getHigh() {
		return  highBid == null ? highAsk : highBid;
	}

	public Double getLow() {
		return   lowBid == null ? lowAsk : lowBid;
	}

	public Double getClose() {
		return   closeBid == null ? closeAsk : closeBid;
	}
	
	public boolean equalsPrices(Candle otherCandle) {
		return openBid != null && openBid.equals(otherCandle.getOpenBid()) && 
				closeBid != null && closeBid.equals(otherCandle.getCloseBid()) && 
				lowBid != null && lowBid.equals(otherCandle.getLowBid()) && 
				highBid != null && highBid.equals(otherCandle.getHighBid());
	}
}

package com.trading.entity;

import java.awt.Color;
import java.time.LocalDateTime;

import org.apache.commons.lang3.BooleanUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.trading.enums.EnumTimeRange;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Entity
@Table(name = "CANDEL")
@IdClass(CandelId.class)
public class Candel {

	@Id
	private LocalDateTime date;
	@Id
	private String market;
	@Id
	@Enumerated(EnumType.STRING)
	private EnumTimeRange timeRange;
	@Column
	private Double lowAsk;
	@Column
	private Double highAsk;
	@Column
	private Double openAsk;
	@Column
	private Double closeAsk;
	@Column
	private Double lowBid;
	@Column
	private Double highBid;
	@Column
	private Double openBid;
	@Column
	private Double closeBid;
	@Column
	private Boolean verified;
	@Transient
	Color color;
	@Transient
	private Integer index;
	@Transient
	private boolean draw;
	@Transient
	private LocalDateTime endDate;
//	@Transient
//	boolean startTrade;
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

	public boolean isBefore(Candel candel) {
		return date.isBefore(candel.getDate());
	}
	
	public boolean isAfter(Candel candel) {
		return date.isAfter(candel.getDate());
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
	
	public CandelId buildId() {
		return new CandelId(market, date, timeRange); 
	}
	
	public boolean isVerified() {
		return BooleanUtils.isTrue(verified); 
	}
	
	public boolean equalsPrices(Candel otherCandel) {
		return openBid != null && openBid.equals(otherCandel.getOpenBid()) && 
				closeBid != null && closeBid.equals(otherCandel.getCloseBid()) && 
				lowBid != null && lowBid.equals(otherCandel.getLowBid()) && 
				highBid != null && highBid.equals(otherCandel.getHighBid());
	}
}

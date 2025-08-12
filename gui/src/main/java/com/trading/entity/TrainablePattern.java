package com.trading.entity;

import java.time.LocalDateTime;

import com.trading.dto.PatternContent;
import com.trading.enums.EnumPatternType;
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
@Entity
@Table(name = "TRAINABLE_PATTERN")
@IdClass(TrainablePatternId.class)
public class TrainablePattern {

	@Id
	String market;
	@Id
	@Enumerated(EnumType.STRING)
	EnumTimeRange timeRange;
	@Id
	@Column(name="DATE_START")
	LocalDateTime dateStart;
	@Id
	@Column(name="DATE_END")
	LocalDateTime dateEnd;
	@Enumerated(EnumType.STRING)
	@Id
	EnumPatternType type;
	@Id
	String source;
	@Column(name="CONTENT")
	String contentJson;
	Boolean processed;
	@Column(name="SENTIMENT")
	private Integer sentiment;
	@Column(name="CREATION_date")
	LocalDateTime creationDate;
	@Column(name="UPDATE_date")
	LocalDateTime updateDate;
	Double prediction;
	@Column
	Double priceEntryZone;
	@Column
	Double riskRatio;
	@Column(name = "trade_id")
	Long tradeId;
	String direction;
	
	@Transient
	private PatternContent content;

	public TrainablePattern copy(){
		TrainablePattern newPat = new TrainablePattern();
		newPat.setMarket(market);
		newPat.setTimeRange(timeRange);
		newPat.setDateStart(dateStart);
		newPat.setDateEnd(dateEnd);
		newPat.setType(type);
		newPat.setSource(source);
		newPat.setContentJson(contentJson);
		newPat.setCreationDate(creationDate);
		newPat.setPrediction(prediction);
		newPat.setPriceEntryZone(priceEntryZone);
		newPat.setContent(content);
		return newPat;

	}

}

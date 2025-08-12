package com.trading.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import com.trading.enums.EnumDirection;
import com.trading.utils.AppUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "TRAINING_TRADE")
public class TrainingTrade {

	@Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column
    UUID id;
	String market;
	String type;
	@Enumerated(EnumType.STRING)
	EnumDirection direction;
	LocalDateTime dateOpen;
	Double priceOpen;
	LocalDateTime dateClose;
	Double priceClose;
	LocalDateTime dateStoploss;
	Double stoploss;
	LocalDateTime dateCreation;
	Double riskRatio;
//	String reason;
//	String source;
//	Double prediction;
//	String timerangeOpen;
//	String timerangeClose;
	
	public Double buildRiskRatio(Double price) {
		Double sl = priceOpen - stoploss;
		Double tp = price - priceOpen;
		if (direction == EnumDirection.SELL) {
			sl = stoploss - priceOpen;
			tp = priceOpen - price;
		}
		Double rr = AppUtils.getRoundedNumber(tp / sl, 1);
		if (rr < -1) {
			return -1d;
		}
		return rr;
	}
}





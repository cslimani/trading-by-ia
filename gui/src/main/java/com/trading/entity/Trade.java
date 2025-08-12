package com.trading.entity;

import java.time.LocalDateTime;

import com.trading.enums.EnumDirection;
import com.trading.enums.EnumTradeStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "TRADE")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

	@Column(name = "id")
	@Id
	Long identifier;
	String market;
	@Enumerated(EnumType.STRING)
	EnumDirection direction;
	LocalDateTime dateCreation;
	LocalDateTime dateOpen;
	Double priceOpen;
	Double stoploss;
	Double originalStoploss;
	LocalDateTime dateClose;
	Double priceClose;
	Double riskRatio;
	@Enumerated(EnumType.STRING)
	EnumTradeStatus status;
	String closingReason;
	Double profitEuro;
	Double volume;
	Double riskEuro;
	Double takeProfit;
//	LocalDateTime dateClose;
//	Double priceClose;
	
	public Double buildRiskRatio(Double price) {
		if (priceOpen != null && direction == EnumDirection.BUY) {
			return (price - priceOpen)/ (priceOpen - originalStoploss);
		}
		if (priceOpen != null && direction == EnumDirection.SELL) {
			return (priceOpen - price)/ (originalStoploss - priceOpen);
		}
		return null;
	}
}

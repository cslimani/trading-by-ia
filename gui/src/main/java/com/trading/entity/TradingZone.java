package com.trading.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "TRADING_ZONE")
public class TradingZone {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;
	@Column(name = "price_start")
	Double priceStart;
	@Column(name = "price_end")	
	Double priceEnd;
	@Column(name = "date_start")
	LocalDateTime dateStart;
	@Column(name = "date_end")
	LocalDateTime dateEnd;	
	
}

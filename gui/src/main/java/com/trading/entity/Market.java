package com.trading.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

@Entity
@Table(name = "MARKET")
@Data
public class Market {

	@Id
	@Column(name = "CODE", nullable = false)
	String code;
	Integer scale;

	@Transient
	boolean openTrade;
	@Transient
	boolean alert;
	@Transient
	Double riskRatio;
}

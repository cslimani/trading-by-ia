package com.trading.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "POINT_OF_INTEREST")
@IdClass(PointOfInterestId.class)
public class PointOfInterest {

	@Id
	private String market;
	@Id
	@Column(name="DATE_MAX")
	private LocalDateTime dateMax;
	@Column(name="DATE_MIN_BEFORE_MAX")
	private LocalDateTime dateMinBeforeMax;
	@Column(name="DATE_MIN_AFTER_MAX")
	private LocalDateTime dateMinAfterMax;
	@Column(name="DONE")
	private boolean done;
	
}

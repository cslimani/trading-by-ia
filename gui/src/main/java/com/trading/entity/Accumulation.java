package com.trading.entity;

import java.time.LocalDateTime;

import com.trading.dto.AccumulationContent;
import com.trading.enums.EnumTimeRange;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "ACCUMULATION")
@IdClass(AccumulationId.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Accumulation {

	@Id
	String market;
	@Id
	@Enumerated(EnumType.STRING)
	EnumTimeRange timeRange;
	@Id
	@Column(name="DATE_START")
	LocalDateTime dateStart;
	@Id
	String type;
//	@Id
	@Column(name="DATE_END")
	LocalDateTime dateEnd;
	@Column(name="CONTENT")
	String contentJson;
	@Column(name="CREATION_date")
	LocalDateTime creationDate;
	@Column(name="STATUS")
	String status;
	@Transient
	private AccumulationContent content;
	@Column(name="INDICATOR")
	Double indicator;

}

package com.trading.entity;

import java.time.LocalDateTime;

import com.trading.enums.EnumTimeRange;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "MOMENT_OF_INTEREST")
@IdClass(MomentOfInterestId.class)
public class MomentOfInterest {

	@Id
	private String market;
	@Id
	@Column(name="DATE_START")
	private LocalDateTime dateStart;
	@Id
	@Column(name="DATE_END")
	private LocalDateTime dateEnd;
	@Enumerated(EnumType.STRING)
	private EnumTimeRange timeRange;
}

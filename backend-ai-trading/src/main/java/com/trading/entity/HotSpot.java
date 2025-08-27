package com.trading.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.trading.enums.EnumTimeRange;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HotSpot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	String market;
	
	@Enumerated(EnumType.STRING)
	EnumTimeRange timeRange;
	
	LocalDateTime dateStart;
	
	LocalDateTime dateEnd;
	
	LocalDateTime creationDate;
	
	String code;
	
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(columnDefinition = "timestamp[]")
	private List<LocalDateTime> keyDates;

}


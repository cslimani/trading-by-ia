package com.trading.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table(name = "TICK_PRICE")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TickPrice {

	@Id
	String id;
	Double ask;
	Double bid;
	String market;
	LocalDateTime date;

}

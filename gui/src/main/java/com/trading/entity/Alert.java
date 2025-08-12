package com.trading.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import com.trading.enums.EnumAlertType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "ALERT")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Alert {
	
	@Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column
    UUID id;
	
	String market;
	Double price;
	LocalDateTime zoneDate;
	Double zonePriceHigh;
	Double zonePriceLow;
	LocalDateTime dateCreation;
	boolean sent;
	@Enumerated(EnumType.STRING)
	EnumAlertType type;
}

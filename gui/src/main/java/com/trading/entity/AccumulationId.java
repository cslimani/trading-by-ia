package com.trading.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.trading.enums.EnumTimeRange;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AccumulationId implements Serializable {

    String market;
    EnumTimeRange timeRange;
	LocalDateTime dateStart;
	String type;
}

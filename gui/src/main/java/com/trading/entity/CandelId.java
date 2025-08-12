package com.trading.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.trading.enums.EnumTimeRange;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandelId implements Serializable {

    private static final long serialVersionUID = 1L;
	String market;
    LocalDateTime date;
    EnumTimeRange timeRange;
}

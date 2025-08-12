package com.trading.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.trading.enums.EnumPatternType;
import com.trading.enums.EnumTimeRange;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TrainablePatternId implements Serializable {

    String market;
    EnumTimeRange timeRange;
	LocalDateTime dateStart;
	LocalDateTime dateEnd;
	EnumPatternType type;
	String source;
}

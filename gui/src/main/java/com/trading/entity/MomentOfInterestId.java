package com.trading.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MomentOfInterestId implements Serializable {

	private String market;
	private LocalDateTime dateStart;
	private LocalDateTime dateEnd;
}

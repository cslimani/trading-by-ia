package com.trading.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum EnumPatternType {

	BOS(5), SUPPLY_ZONE(2);
	
	private int nbPoints;
}

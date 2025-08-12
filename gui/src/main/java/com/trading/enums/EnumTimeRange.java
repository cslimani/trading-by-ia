package com.trading.enums;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

@Getter
public enum EnumTimeRange {

	S15("S15", 15, 15), 
	S30("S30", 30, 30), 
	M1("1MIN", 1, 60),
	M2("2MIN", 2, 120),
	M3("3MIN", 3, 180),
	M5("5MIN", 5, 300),
	M10("10MIN", 10, 600),
	M15("15MIN", 15, 900), 
	M30("30MIN", 30, 1800),
	H1("1HOUR", 1, 3600),
	H4("4HOUR", 4, 14400);

	private static final Map<String, EnumTimeRange> map;
	static {
		map = new HashMap<String, EnumTimeRange>();
		for (EnumTimeRange tr : EnumTimeRange.values()) {
			map.put(tr.code, tr);
		}
	}

	public static EnumTimeRange getByCode(String c) {
		return map.get(c);
	}

	private String code;
	private int value;
	private int nbSeconds;
	
	private EnumTimeRange(String code, int value, int nbSeconds) {
		this.code = code;
		this.value = value;
		this.nbSeconds = nbSeconds;
	}
	
	public EnumTimeRange getHigher() {
		return EnumTimeRange.values()[this.ordinal()+1];
	}
	
	public List<EnumTimeRange> getLowers() {
		return    Arrays.stream(EnumTimeRange.values()).filter(tr -> tr.ordinal() < ordinal()).toList();
	}
	
	public List<EnumTimeRange> getHighers() {
		return    Arrays.stream(EnumTimeRange.values()).filter(tr -> tr.ordinal() > ordinal()).toList();
	}
	
}

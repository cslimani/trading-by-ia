package com.trading.service;

import java.time.LocalDateTime;

import com.trading.dto.Range;
import com.trading.entity.Candle;

public class DebugHolder {

	private static boolean debug;
//	private static LocalDateTime date = LocalDateTime.of(2023, 9, 12, 2, 30);
	private static LocalDateTime date = LocalDateTime.of(2021, 5, 27, 13, 30);
	private static Candle candleDate = null;
			
	public static void activate(Candle c){
//		System.out.println(c.getDate());
		if (c.getDate().isEqual(date)) {
//			candleDate = c;
//			debug = true;
		}
	}

	public static void eliminated(String reason) {
		if (debug) {
			System.out.println("Eliminated because " + reason);
		}
	}

	public static void info() {
		if (debug) {
			date.getClass();
		}
	}
	
	public static void message(String message) {
		if (debug) {
			System.out.println(message);
		}
	}

	public static void accepted(Range range) {
//		System.out.println("Range found starting at " + range.getDateStart().format(AbstractService.formatter));
//		if (candleDate != null && Math.abs(range.getIndexStart() - candleDate.getIndex()) < 20) {
//			System.out.println("Range found starting at " + range.getDateStart());
//		}
		if (debug) {
			date.getClass();
		}
	}
}

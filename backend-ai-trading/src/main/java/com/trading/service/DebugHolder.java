package com.trading.service;

import java.time.LocalDateTime;
import java.time.Month;

public class DebugHolder {

	private static boolean debug;
	private static LocalDateTime date = LocalDateTime.of(2020, Month.MAY, 27, 10, 40);
	
	public static void activate(LocalDateTime d){
		if (d.isEqual(date)) {
//			debug = true;
		}
	}

	public static void eliminated() {
		if (debug) {
			System.out.println();
		}
	}

	public static void info() {
		if (debug) {
			System.out.println();
		}
	}
	
	public static void message(String message) {
		if (debug) {
			System.out.println(message);
		}
	}
}

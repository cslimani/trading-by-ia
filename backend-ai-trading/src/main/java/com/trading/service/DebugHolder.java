package com.trading.service;

import java.time.LocalDateTime;
import java.time.Month;

public class DebugHolder {

	private static boolean debug;
	private static LocalDateTime date = LocalDateTime.of(2023, Month.AUGUST, 3, 5, 0);
	
	public static void activate(LocalDateTime d){
		if (d.isEqual(date)) {
			debug = true;
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

	public static void accepted() {
		if (debug) {
			date.getClass();
		}
	}
}

package com.trading.utils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import com.trading.enums.EnumTimeRange;

public class DateHelper {
	public static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	public static DateTimeFormatter dtf_DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	public static DateTimeFormatter DTF_HOUR = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");
	private static Set<LocalDate> bankHolidays = new  HashSet<LocalDate>();
	
	static {
		//2019
		bankHolidays.add(LocalDate.of(2019, 12, 24));
		bankHolidays.add(LocalDate.of(2019, 12, 25));
		bankHolidays.add(LocalDate.of(2019, 12, 26));
		bankHolidays.add(LocalDate.of(2019, 12, 31));

		//2020
		bankHolidays.add(LocalDate.of(2020, 1, 1));
		bankHolidays.add(LocalDate.of(2020, 4, 10));
		bankHolidays.add(LocalDate.of(2020, 4, 13));
		bankHolidays.add(LocalDate.of(2020, 5, 1));
		bankHolidays.add(LocalDate.of(2020, 12, 24));
		
		//2021
		bankHolidays.add(LocalDate.of(2021, 1, 1));
	}
	public static LocalDateTime parse(String value) {
		return  LocalDateTime.parse(value, dtf) ;
	}

	public static String format(LocalDateTime date) {
		return dtf.format(date);
	}

	public static Instant getInstant(LocalDateTime date) {
		return date.atZone(ZoneId.systemDefault()).toInstant();
	}

	public static long getInstantMilliseconds(LocalDateTime date) {
		return date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	public static LocalDateTime toLocalDateTime(long time) {
		return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

//	public static boolean isTradingHour(LocalTime date, Market market) {
//		String tradingHours = market.getTradingHours();
//		LocalTime localTime = LocalTime.of(0, 0);
//		if (tradingHours != null) {
//			String[] intervals = tradingHours.split(";");
//			for (String interval : intervals) {
//				String[] hours = interval.split("-");
//				LocalTime start = localTime.plusSeconds(Integer.valueOf(hours[0]));
//				LocalTime end = localTime.plusSeconds(Integer.valueOf(hours[1]));
//				if (start.equals(end) || (date.isAfter(start) || date.equals(start)) && date.isBefore(end)) {
//					return true;
//				}
//			}
//		}
//		return false;
//	}

	public static LocalDateTime getTruncatedDate(LocalDateTime date, EnumTimeRange timeRange) {
		if (timeRange.getCode().startsWith("S")) {
			LocalDateTime dateTrunc = date.truncatedTo(ChronoUnit.SECONDS);
			return dateTrunc.withSecond((dateTrunc.getSecond() / timeRange.getValue()) * timeRange.getValue());
		} else if (timeRange.getCode().contains("MIN")) {
			LocalDateTime dateTrunc = date.truncatedTo(ChronoUnit.MINUTES);
			return dateTrunc.withMinute((dateTrunc.getMinute() / timeRange.getValue()) * timeRange.getValue());
		} else if (timeRange.getCode().contains("HOUR")) {
			LocalDateTime dateTrunc = date.truncatedTo(ChronoUnit.HOURS);
			return dateTrunc.withHour((dateTrunc.getHour() / timeRange.getValue()) * timeRange.getValue());
		} else {
			throw new IllegalArgumentException("Unknown timerange " + timeRange);
		}
	}

//	public static LocalDateTime getNextTradingHour(LocalDateTime date, EnumTimeRange timeRange, MarketDataDTO marketData) {
//		date = getTruncatedDate(date, timeRange);
//		date = date.plusMinutes(timeRange.getValue());
//		while(!getTradingHoursList(marketData).contains(date.toLocalTime()) || DateHelper.isWeekend(date) || DateHelper.isBankHoliday(date)) {
//			date = date.plusMinutes(timeRange.getValue());
//		}
//		return date;
//	}
	
	public static String format_DD_MM_YYYY(LocalDateTime date) {
		return dtf_DDMMYYYY.format(date);
	}

	public static boolean isWeekend(LocalDateTime date) {
		return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;

	}

	public static LocalDate getPreviousTradingDay(LocalDate date) {
		LocalDate dayBefore = date.minusDays(1);
		while (bankHolidays.contains(dayBefore) || dayBefore.getDayOfWeek() == DayOfWeek.SATURDAY || dayBefore.getDayOfWeek() == DayOfWeek.SUNDAY) {
			dayBefore = dayBefore.minusDays(1);
		}
		return dayBefore;
	}

	public static boolean isBankHoliday(LocalDateTime date) {
		return isBankHoliday(date.toLocalDate());
	}

	public static boolean isBankHoliday(LocalDate date) {
		return bankHolidays.contains(date);
	}
	
//	public static List<LocalTime> getTradingHoursList(MarketDataDTO marketData) {
//		if (marketData.getTradingHoursList() != null) {
//			return marketData.getTradingHoursList(); 
//		}
//		ArrayList<LocalTime> tradingHoursList = new ArrayList<LocalTime>();
//		LocalTime start = LocalTime.of(0, 0);
//		for (int i = 0; i < NB_MINUTES_PER_DAY; i++) {
//			if (DateHelper.isTradingHour(start, marketData.getMarket())) {
//				tradingHoursList.add(start);
//			}
//			start = start.plusMinutes(1);
//		}
//		marketData.setTradingHoursList(tradingHoursList);
//		return tradingHoursList;
//	}
	
	public static String toStringGMT(LocalDateTime date) {
		return DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(date.atZone(ZoneId.of("Europe/Paris")).toInstant());
	}

	public static LocalDateTime toLocalDateTime(Instant date) {
		return LocalDateTime.ofInstant(date, ZoneId.of("Europe/Paris"));
	}
}

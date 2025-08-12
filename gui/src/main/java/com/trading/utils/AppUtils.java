package com.trading.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.trading.entity.Candel;
import com.trading.entity.Market;

public class AppUtils {

	private static Map<String, LocalTime> mapActionTime = new ConcurrentHashMap<String, LocalTime>();

	public static Double round(double d, int decimalPlace) {
		BigDecimal bd = new BigDecimal(d);
		bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}

	public static Double getRoundedNumber(Double value) {
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(6, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	public static Double getRoundedNumber(Double value, int scale) {
		if (value == null) {
			return null;
		}
		try {
			BigDecimal bd = new BigDecimal(value);
			bd = bd.setScale(scale, RoundingMode.HALF_UP);
			return bd.doubleValue();
		} catch (Exception e) {
			System.out.println(e);
		}
		return 0d;
	}

	public static String toString(Double value) {
		if (value != null) {
			return new DecimalFormat("#.######").format(value);
		}
		return null;
	}

	public static boolean onlyOncePerMinute(Market market, String key) {
		LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
		String fullKey = market.getCode() + "-" + key;
		LocalTime value = mapActionTime.get(fullKey);
		if (value == null || !value.equals(now)) {
			mapActionTime.put(fullKey, now);
			return true;
		} else {
			return false;
		}
	}

	public static boolean isWeekend(LocalDateTime date) {
		return date.getDayOfWeek() == DayOfWeek.SATURDAY
				|| (date.getDayOfWeek() == DayOfWeek.FRIDAY && date.getHour() >= 22)
				|| (date.getDayOfWeek() == DayOfWeek.SUNDAY && date.getHour() < 23);

	}

	public static Candel getMin(int startIndex, int endIndex, List<Candel> candels) {
		Candel min = null;
		for (int i = Math.max(startIndex, 0); i <= endIndex; i++) {
			Candel c = candels.get(i);
			if (min == null || c.getLow() < min.getLow()) {
				min = c;
			}
		}
		return min;
	}

	public static Candel getMax(int startIndex, int endIndex, List<Candel> candels) {
		Candel max = null;
		for (int i = Math.max(startIndex, 0); i <= endIndex; i++) {
			Candel c = candels.get(i);
			if (max == null || c.getHigh() > max.getHigh()) {
				max = c;
			}
		}
		return max;
	}
	
//	public static double getProfit(TickPrice tickPrice) {
//		double profit = 0d;
//		Market market = trade.getMarket();
//		if (trade.getDirection() == EnumDirection.BUY) {
//			profit = (tickPrice.getBid() - trade.getCreatedLevel()) * market.getRatio();
//		} else {
//			profit = (trade.getCreatedLevel() - tickPrice.getAsk()) * market.getRatio();
//		}
//		return profit;
//	}

}

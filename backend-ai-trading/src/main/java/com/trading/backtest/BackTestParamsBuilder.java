package com.trading.backtest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BackTestParamsBuilder {

	private final Map<String, List<Object>> paramMap = new LinkedHashMap<>();

	public static BackTestParamsBuilder builder() {
		return new BackTestParamsBuilder();
	}

	public BackTestParamsBuilder addDouble(String name, double start, double end, double step) {
		List<Object> values = new ArrayList<>();
		if (step <= 0 && start == end) {
			values.add(start);
		} else if (step > 0) {
			for (double v = start; v <= end; v += step) {
				values.add(v);
			}
		} else {
			throw new RuntimeException("Error with step 0");
		}
		paramMap.put(name, values);
		return this;
	}

	public BackTestParamsBuilder addInteger(String name, int start, int end, int step) {
		List<Object> values = new ArrayList<>();
		if (step <= 0 && start == end) {
			values.add(start);
		} else if (step > 0) {
			for (int v = start; v <= end; v += step) {
				values.add(v);
			}
		} else {
			throw new RuntimeException("Error with step 0");
		}
		paramMap.put(name, values);
		return this;
	}

	public List<BackTestParams> build() {
		List<BackTestParams> result = new ArrayList<>();
		List<String> keys = new ArrayList<>(paramMap.keySet());
		generateRecursive(result, new BackTestParams(), keys, 0);
		return result;
	}

	private void generateRecursive(List<BackTestParams> result, BackTestParams current, List<String> keys, int index) {
		if (index == keys.size()) {
			try {
				BackTestParams copy = new BackTestParams();
				for (Field field : BackTestParams.class.getDeclaredFields()) {
					field.set(copy, field.get(current));
				}
				result.add(copy);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			return;
		}

		String key = keys.get(index);
		List<Object> values = paramMap.get(key);

		for (Object val : values) {
			try {
				Field field = BackTestParams.class.getDeclaredField(key);
				field.set(current, val);
				generateRecursive(result, current, keys, index + 1);
			} catch (Exception e) {
				throw new RuntimeException("Error setting field: " + key, e);
			}
		}
	}
}

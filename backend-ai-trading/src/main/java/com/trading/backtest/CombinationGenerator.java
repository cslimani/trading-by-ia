package com.trading.backtest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CombinationGenerator {

	public static List<BackTestParams> generateCombinations(String input) throws Exception {
		Map<String, List<Object>> paramValuesMap = new LinkedHashMap<>();
		String[] paramDefs = input.split(";");

		for (String def : paramDefs) {
			String[] parts = def.split("=");
			String paramName = parts[0];
			String[] rangeParts = parts[1].split(",");

			// Détection du type avec reflection
			Field field = BackTestParams.class.getDeclaredField(paramName);
			Class<?> type = field.getType();

			if (type == Double.class) {
				double start = Double.parseDouble(rangeParts[0]);
				double end = Double.parseDouble(rangeParts[1]);
				double step = Double.parseDouble(rangeParts[2]);

				List<Object> values = new ArrayList<>();
				for (double v = start; v <= end ; v += step) {
					values.add(v);
				}
				paramValuesMap.put(paramName, values);

			} else if (type == Integer.class) {
				int start = Integer.parseInt(rangeParts[0]);
				int end = Integer.parseInt(rangeParts[1]);
				int step = Integer.parseInt(rangeParts[2]);

				List<Object> values = new ArrayList<>();
				for (int v = start; v <= end; v += step) {
					values.add(v);
				}
				paramValuesMap.put(paramName, values);

			} else {
				throw new IllegalArgumentException("Type non supporté : " + type);
			}
		}

		List<BackTestParams> results = new ArrayList<>();
		generateRecursive(paramValuesMap, new ArrayList<>(paramValuesMap.keySet()), 0, new BackTestParams(), results);

		return results;
	}

	private static void generateRecursive(Map<String, List<Object>> paramMap,
			List<String> keys,
			int index,
			BackTestParams current,
			List<BackTestParams> results) throws Exception {
		if (index == keys.size()) {
			BackTestParams clone = new BackTestParams();
			for (Field field : BackTestParams.class.getDeclaredFields()) {
				field.set(clone, field.get(current));
			}
			results.add(clone);
			return;
		}

		String key = keys.get(index);
		List<Object> values = paramMap.get(key);

		for (Object val : values) {
			Field field = BackTestParams.class.getDeclaredField(key);
			field.set(current, val);
			generateRecursive(paramMap, keys, index + 1, current, results);
		}
	}

	public static void main(String[] args) throws Exception {
		String input = "stopLoss=0,0.1,0.05;buyLimit=0,0.2,0.1;nbTop=1,3,1";
		List<BackTestParams> combinations = generateCombinations(input);

		for (BackTestParams p : combinations) {
			System.out.println(p);
		}

		System.out.println("Total: " + combinations.size());
	}
}

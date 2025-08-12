package com.trading.indicator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.trading.entity.Candel;
import com.trading.enums.EnumTimeRange;
import com.trading.utils.AppUtils;

public class TryIndicatorSurface implements Indicator{

	private static final Integer NB_STEPS = 20;
	private static final int NB_CANDELS = 200;
	Map<Integer, Double> map = new HashMap<>();
	Map<LocalDateTime, Double> mapDate = new HashMap<>();
	List<Candel> candels;
	private int nbPeriods;
	private EnumTimeRange tr;

	public TryIndicatorSurface(List<Candel> candels, int nbPeriods, EnumTimeRange tr) {
		this.candels = candels;
		this.nbPeriods = nbPeriods;
		this.tr = tr;
		init();
	}

	private void init() {
		List<Double> values = new ArrayList<Double>();
		for (int index = 1000; index < candels.size(); index++) {
			Candel candel = candels.get(index);
			Double totalSurface = getTotalSurface(candel);
			Double surface = 0d;
			Double delta = candel.getHigh() - candel.getLow();
			Double deltaOneStep = delta/NB_STEPS;
			for (int step = 1; step <= NB_STEPS; step++) {
				Double value = candel.getHigh() - step*deltaOneStep;
				int nbCandels = NB_CANDELS;
				for (int p = 1; p < NB_CANDELS; p++) {
					if (candels.get(index-p).getLow() <= value) {
						nbCandels = p;
						break;
					}
				}
				surface += nbCandels*deltaOneStep;
			}
			values.add(100*surface/totalSurface);
			if (values.size() > nbPeriods) {
				Double sum = 0d;
				for (int i = 0; i < nbPeriods; i++) {
					sum += values.get(values.size()-1-i);
				}
				System.out.println("Putting indicator for " + candel.getDate());
				mapDate.put(candel.getDate(), sum);
			}
		}

		System.out.println("TryIndicatorSurface finished init");
	}

	//	private Double getTotalSurface(Candel candel) {
	//		Candel min = getMin(candel.getIndex()-NB_CANDELS, candel.getIndex());
	//		Candel max = getMax(candel.getIndex()-NB_CANDELS, candel.getIndex());
	//		return (max.getHigh() - min.getLow())*NB_CANDELS;
	//	}

	private Double getTotalSurface(Candel candel) {
		Candel min = AppUtils.getMin(candel.getIndex()-NB_CANDELS, candel.getIndex(), candels);
		Double surface = 0d;
		for (int i = 0; i < NB_CANDELS; i++) {
			surface += candels.get(candel.getIndex()-i).getHigh() - min.getLow();
		}
		return surface;
	}

	

	public Double get(Integer index) {
		return map.get(index);
	}

	public Double get(LocalDateTime date) {
		return mapDate.get(date);
	}

}

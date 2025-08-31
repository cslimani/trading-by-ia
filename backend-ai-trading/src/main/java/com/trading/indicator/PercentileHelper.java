package com.trading.indicator;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.trading.entity.Candle;
import com.trading.launcher.BasicAccumulation.PercentileResult;

public class PercentileHelper {

	public static PercentileResult percentileQuickSelect(List<Candle> candles, double p, boolean high) {
	    if (Double.isNaN(p) || p < 0.0 || p > 100.0) {
	        throw new IllegalArgumentException("Le centile doit être entre 0 et 100.");
	    }
	    final int n = candles.size();
	    if (n == 0) throw new IllegalArgumentException("Aucune bougie.");

	    // Rang comme dans ton code (ceil) puis bornage dans [0, n-1]
	    int k = (int) Math.ceil((p / 100.0) * n) - 1;
	    if (k < 0) k = 0;
	    if (k >= n) k = n - 1;

	    // Tableaux primitifs pour limiter la GC
	    double[] vals = new double[n];
	    int[] idx = new int[n];

	    for (int i = 0; i < n; i++) {
	        Candle c = candles.get(i);
	        double v = high ? c.high : c.low; // adapte si tes getters diffèrent
	        if (Double.isNaN(v)) throw new IllegalArgumentException("Valeur NaN rencontrée.");
	        vals[i] = v;
	        idx[i] = c.getIndex(); // ou simplement i si tu veux l'index dans la liste
	    }

	    quickSelect(vals, idx, 0, n - 1, k);
	    return new PercentileResult(vals[k], idx[k]);
	}

	private static void quickSelect(double[] a, int[] idx, int left, int right, int k) {
	    ThreadLocalRandom rnd = ThreadLocalRandom.current();
	    while (left < right) {
	        int pivot = left + rnd.nextInt(right - left + 1);
	        int p = partition(a, idx, left, right, pivot);
	        if (k == p) return;
	        if (k < p) right = p - 1; else left = p + 1;
	    }
	}

	private static int partition(double[] a, int[] idx, int left, int right, int pivot) {
	    double pv = a[pivot];
	    swap(a, idx, pivot, right);
	    int store = left;
	    for (int i = left; i < right; i++) {
	        if (a[i] < pv) {
	            swap(a, idx, store, i);
	            store++;
	        }
	    }
	    swap(a, idx, store, right);
	    return store;
	}

	private static void swap(double[] a, int[] idx, int i, int j) {
	    if (i == j) return;
	    double dv = a[i]; a[i] = a[j]; a[j] = dv;
	    int di = idx[i]; idx[i] = idx[j]; idx[j] = di;
	}
}

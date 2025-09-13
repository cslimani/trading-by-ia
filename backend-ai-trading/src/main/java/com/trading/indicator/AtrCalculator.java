package com.trading.indicator;

import java.util.List;

import com.trading.entity.Candle;

public final class AtrCalculator {

    private AtrCalculator() {}

    public static void compute(List<Candle> candles) {
        compute(candles, 14);
    }

    public static void compute(List<Candle> candles, int period) {
        if (candles == null || candles.isEmpty()) return;
        if (period <= 0) throw new IllegalArgumentException("period must be > 0");

        // 1) True Range (TR) pour chaque barre
        Double prevClose = null;
        for (int i = 0; i < candles.size(); i++) {
            Candle c = candles.get(i);
            double tr;
            if (prevClose == null) {
                // première barre : TR = H - L
                tr = c.high - c.low;
            } else {
                double hMinusL = c.high - c.low;
                double hMinusPrevC = Math.abs(c.high - prevClose);
                double lMinusPrevC = Math.abs(c.low - prevClose);
                tr = Math.max(hMinusL, Math.max(hMinusPrevC, lMinusPrevC));
            }
            c.tr = tr;
            // on initialisera atr/atr_ratio après
            prevClose = c.close;
        }

        // 2) ATR (Wilder). Initialisation à la moyenne des 'period' premiers TR.
        if (candles.size() < period) {
            // Pas assez de barres pour initialiser l'ATR : on met NaN pour ATR et ATR%.
            for (Candle c : candles) {
                c.atr = c.tr;
                c.atr_ratio = Double.NaN;
            }
            return;
        }

        double sumTR = 0.0;
        for (int i = 0; i < period; i++) {
            sumTR += candles.get(i).tr;
            // ATR non défini pour les barres < period-1
            candles.get(i).atr = candles.get(i).tr;
            candles.get(i).atr_ratio = Double.NaN;
        }
        double atr = sumTR / period;

        // On pose l'ATR à t = period-1 (barre n-1, en 0-based)
        Candle c0 = candles.get(period - 1);
        c0.atr = atr;
        c0.atr_ratio = safeDivide(atr, c0.close);

        // 3) Lissage de Wilder pour les barres suivantes
        for (int i = period; i < candles.size(); i++) {
            Candle c = candles.get(i);
            double tr = c.tr;
            atr = ((period - 1) * atr + tr) / period;  // RMA / Wilder
            c.atr = atr;
            c.atr_ratio = safeDivide(atr, c.close);
        }
    }

    private static double safeDivide(double num, double den) {
        return (den == 0.0 || Double.isNaN(den)) ? Double.NaN : (num / den);
    }

	public static Double average(List<Candle> candles, int indexEnd, int period) {
		double sum = 0.0;
		for (int i = indexEnd - period; i < indexEnd; i++) {
			sum += candles.get(i).getAtr();
		}
		return sum / period;
	}
}


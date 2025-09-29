package com.trading.indicator;

import java.util.List;

import com.trading.entity.Candle;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AtrCalculator {

	Double prevClose = null;
	double sumTR = 0.0;
	private final Integer period;
	private double atr;

	public void compute(Candle c) {
		if (period <= 0) throw new IllegalArgumentException("period must be > 0");

		// 1) True Range (TR) pour chaque barre

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


		if (c.getIndex() < period) {
			sumTR += c.tr;
			c.atr = c.tr;
			c.atr_ratio = Double.NaN;
		} 
		

		// On pose l'ATR à t = period-1 (barre n-1, en 0-based)
		if (c.index == period - 1) {
			atr = sumTR / period;
			c.atr = atr;
			c.atr_ratio = safeDivide(atr, c.close);
		} else if (c.index >= period){
			// 3) Lissage de Wilder pour les barres suivantes
			atr = ((period - 1) * atr + c.tr) / period;  // RMA / Wilder
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


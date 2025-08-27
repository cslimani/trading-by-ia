package com.trading.indicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.trading.entity.Candle;

import lombok.Getter;

@Getter
public final class AtrCalculator {

    private Map<Integer, Double> mapTR = new HashMap<Integer, Double>();
    private Map<Integer, Double> mapATR = new HashMap<Integer, Double>();
    
    public  AtrCalculator(List<Candle> candles) {
        compute(candles, 14);
    }

    public void compute(List<Candle> candles, int period) {
        if (candles == null || candles.isEmpty()) return;
        if (period <= 0) throw new IllegalArgumentException("period must be > 0");

        // 1) True Range (TR) pour chaque barre
        Double prevClose = null;
        for (int i = 0; i < candles.size(); i++) {
            Candle c = candles.get(i);
            double tr;
            if (prevClose == null) {
                // première barre : TR = H - L
                tr = c.getHigh() - c.getLow();
            } else {
                double hMinusL = c.getHigh() - c.getLow();
                double hMinusPrevC = Math.abs(c.getHigh() - prevClose);
                double lMinusPrevC = Math.abs(c.getLow() - prevClose);
                tr = Math.max(hMinusL, Math.max(hMinusPrevC, lMinusPrevC));
            }
            mapTR.put(c.getIndex(), tr);
            prevClose = c.getClose();
        }

        // 2) ATR (Wilder). Initialisation à la moyenne des 'period' premiers TR.
        if (candles.size() < period) {
            // Pas assez de barres pour initialiser l'ATR : on met NaN pour ATR et ATR%.
            for (Candle c : candles) {
            	mapATR.put(c.getIndex(), null);
            }
            return;
        }

        double sumTR = 0.0;
        for (int i = 0; i < period; i++) {
            sumTR += mapTR.get(i);
            // ATR non défini pour les barres < period-1
            mapATR.put(i, null);
        }
        double atr = sumTR / period;

        // On pose l'ATR à t = period-1 (barre n-1, en 0-based)
        mapATR.put(period - 1, atr);

        // 3) Lissage de Wilder pour les barres suivantes
        for (int i = period; i < candles.size(); i++) {
            double tr = mapTR.get(i);
            atr = ((period - 1) * atr + tr) / period;  // RMA / Wilder
            mapATR.put(i, atr);
        }
    }

	public Double getATR(Integer index) {
		return mapATR.get(index);
	}

}


package com.trading.indicator;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.trading.entity.Candle;

public final class SlopeLRCalculator {

    private final Map<Integer, Double> mapSlopeLR = new HashMap<>();

    public SlopeLRCalculator(List<Candle> candles) {
        compute(candles, 50);
    }

    public SlopeLRCalculator() {}

    public void compute(List<Candle> candles, int period) {
        mapSlopeLR.clear();
        if (candles == null || candles.isEmpty()) return;
        if (period < 2) throw new IllegalArgumentException("period must be >= 2");

        final int n = period;

        // Constantes pour la régression LR avec x = 0..n-1
        // sumX = n(n-1)/2 ; sumX2 = (n-1)n(2n-1)/6 ; denom = n*sumX2 - (sumX)^2
        final double sumX  = n * (n - 1) / 2.0;
        final double sumX2 = (n - 1) * n * (2.0 * n - 1.0) / 6.0;
        final double denom = n * sumX2 - sumX * sumX; // > 0 si n >= 2

        // Fenêtre glissante des closes + sommes nécessaires
        Deque<Double> window = new ArrayDeque<>(n);
        double sumY = 0.0;   // somme des y (closes) de la fenêtre
        double sumXY = 0.0;  // somme des j*y_j avec j = 0..n-1 (poids LR)

        for (int i = 0; i < candles.size(); i++) {
            double y = candles.get(i).getClose();

            if (window.size() < n) {
                // Construction initiale : on ajoute y avec poids = index courant dans la fenêtre
                int j = window.size();
                window.addLast(y);
                sumY  += y;
                sumXY += j * y;

                if (window.size() < n) {
                    mapSlopeLR.put(i, null); // pas assez d'historique
                    continue;
                }

                // Fenêtre tout juste remplie : on peut calculer la pente
                double slope = (n * sumXY - sumX * sumY) / denom;
                mapSlopeLR.put(i, slope);
            } else {
                // Mise à jour glissante O(1) :
                // Ancienne fenêtre : y0..y_{n-1}, S = sumY, SX = sumXY
                // Nouvelle fenêtre après décalage + ajout y :
                // SX' = SX - (S - y0) + (n-1)*y
                double y0 = window.removeFirst();
                double newSumXY = sumXY - (sumY - y0) + (n - 1) * y;
                double newSumY  = sumY - y0 + y;

                window.addLast(y);
                sumXY = newSumXY;
                sumY  = newSumY;

                double slope = (n * sumXY - sumX * sumY) / denom;
                mapSlopeLR.put(i, slope);
            }
        }

        // Pour les toutes premières barres (< period-1), s'assurer que la map contient bien null
        for (int i = 0; i < Math.min(period - 1, candles.size()); i++) {
            mapSlopeLR.putIfAbsent(i, null);
        }
    }

    public Double getSlopeLR(Integer index) {
        return mapSlopeLR.get(index);
    }

    public Map<Integer, Double> getAllSlopeLR() {
        return Collections.unmodifiableMap(mapSlopeLR);
    }
}

package com.trading.indicator;

import java.util.List;
import java.util.Objects;

import com.trading.entity.Candle;

public class MacdCalculator {

    public static void computeMacd(List<Candle> candles) {
        computeMacd(candles, 12, 26, 9);
    }

    public static void computeMacd(List<Candle> candles, int shortPeriod, int longPeriod, int signalPeriod) {
        Objects.requireNonNull(candles, "candles");

        if (candles.isEmpty()) return;
        if (shortPeriod <= 0 || longPeriod <= 0 || signalPeriod <= 0)
            throw new IllegalArgumentException("Les périodes doivent être > 0");
        if (shortPeriod >= longPeriod)
            throw new IllegalArgumentException("shortPeriod doit être < longPeriod");

        final double kShort = 2.0 / (shortPeriod + 1.0);
        final double kLong  = 2.0 / (longPeriod + 1.0);
        final double kSig   = 2.0 / (signalPeriod + 1.0);

        Double emaShort = null;
        Double emaLong  = null;

        // 1) Initialiser EMA short/long par SMA sur les N premières clôtures
        emaShort = smaOfLastNClosings(candles, 0, shortPeriod);
        emaLong  = smaOfLastNClosings(candles, 0, longPeriod);

        // Marquer les bougies avant l'init comme null
        for (int i = 0; i < candles.size(); i++) {
            Candle c = candles.get(i);

            // Mettre à jour EMA short si possible
            if (i >= shortPeriod - 1) {
                if (i == shortPeriod - 1) {
                    // déjà initialisée par SMA
                } else {
                    emaShort = (c.close - emaShort) * kShort + emaShort;
                }
            } else {
                c.macd = null;
                c.macdSignal = null;
                continue;
            }

            // Mettre à jour EMA long si possible
            if (i >= longPeriod - 1) {
                if (i == longPeriod - 1) {
                    // déjà initialisée par SMA
                } else {
                    emaLong = (c.close - emaLong) * kLong + emaLong;
                }
            } else {
                // On n'a pas encore EMA long => pas de MACD
                c.macd = null;
                c.macdSignal = null;
                continue;
            }

            // 2) MACD = EMA(short) - EMA(long)
            double macdValue = emaShort - emaLong;
            c.macd = macdValue;

            // 3) Ligne de signal = EMA de la MACD sur signalPeriod
            // Elle commence quand on dispose d'au moins signalPeriod valeurs MACD
            if (i >= (longPeriod - 1) + (signalPeriod - 1)) {
                // initialisation de l'EMA signal par SMA des 'signalPeriod' dernières MACD
                if (i == (longPeriod - 1) + (signalPeriod - 1)) {
                    double smaSignal = smaOfMacd(candles, i - (signalPeriod - 1), signalPeriod);
                    c.macdSignal = smaSignal;
                } else {
                    // Continuer l'EMA signal : emaSignal[n] = (MACD - prev)*k + prev
                    Double prevSignal = candles.get(i - 1).macdSignal;
                    double emaSignal = (macdValue - prevSignal) * kSig + prevSignal;
                    c.macdSignal = emaSignal;
                }
            } else {
                c.macdSignal = null;
            }
//            System.out.println(c.date + " " + c.getMacdDiff());
        }
    }

    // SMA sur 'period' clôtures, à partir de l'indice start inclus (utilisée pour init des EMA short/long)
    private static Double smaOfLastNClosings(List<Candle> candles, int start, int period) {
        if (candles.size() < period) return null;
        double sum = 0.0;
        for (int i = start; i < period; i++) {
            sum += candles.get(i).close;
        }
        return sum / period;
    }

    // SMA des MACD sur 'period' valeurs, terminant à l'indice 'end' (utilisée pour init de la signal)
    private static double smaOfMacd(List<Candle> candles, int startIndex, int period) {
        double sum = 0.0;
        for (int i = startIndex; i < startIndex + period; i++) {
            sum += candles.get(i).macd;
        }
        return sum / period;
    }

}
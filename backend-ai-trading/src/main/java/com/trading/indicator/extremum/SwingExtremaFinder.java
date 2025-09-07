package com.trading.indicator.extremum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.trading.entity.Candle;
import com.trading.enums.ExtremumType;

/**
 * Détecte les extrêmes (swing highs / swing lows) dans une série de bougies.
 * Règles :
 *  - Un MAX : les N bougies AVANT et APRÈS ont toutes un high strictement inférieur au high de la bougie i.
 *  - Un MIN : les N bougies AVANT et APRÈS ont toutes un low  strictement supérieur au low  de la bougie i.
 *  - Il doit y avoir ALTERNANCE des types : MIN puis MAX puis MIN, etc. (ou l’inverse selon le premier qui apparaît).
 */
public class SwingExtremaFinder {

    public enum Type { MIN, MAX }


    public List<Extremum> findExtrema(List<Candle> candles, int N) {
    	return findExtrema(candles, N, true);
    }
    
    public List<Extremum> findExtrema(List<Candle> candles, int N, boolean enforceAlternation) {
        if (candles == null || candles.size() < 2 * N + 1) return Collections.emptyList();

        List<Extremum> candidates = new ArrayList<>();
        final int last = candles.size() - 1;

        // 1) Candidats bruts selon les définitions
        for (int i = N; i <= last - N; i++) {
            Candle c = candles.get(i);
            boolean isMax = true;
            for (int k = i - N; k <= i + N; k++) {
                if (k == i) continue;
                if (candles.get(k).getHigh() > c.getHigh()) { // strictement inférieur requis
                    isMax = false; break;
                }
            }

            boolean isMin = true;
            for (int k = i - N; k <= i + N; k++) {
                if (k == i) continue;
                if (candles.get(k).getLow() < c.getLow()) { // strictement supérieur requis
                    isMin = false; break;
                }
            }

            if (isMax) {
                candidates.add(new Extremum(c, c.getDate(), c.getHigh(), ExtremumType.MAX, N));
            }
            if (isMin) {
                candidates.add(new Extremum(c, c.getDate(), c.getLow(), ExtremumType.MIN, N));
            }
        }

        candidates.sort(Comparator.comparingInt(e -> e.candle.index));

        if (enforceAlternation) {
        	return enforceAlternation(candidates);
        } else {
        	return candidates;
        }
    }

    private static List<Extremum> enforceAlternation(List<Extremum> sortedCandidates) {
        if (sortedCandidates.isEmpty()) return sortedCandidates;

        List<Extremum> out = new ArrayList<>();
        out.add(sortedCandidates.get(0));

        for (int i = 1; i < sortedCandidates.size(); i++) {
            Extremum prev = out.get(out.size() - 1);
            Extremum cur  = sortedCandidates.get(i);

            if (prev.type == cur.type) {
                // Conflit d’alternance → on garde le plus "fort"
                boolean keepCur =
                        (cur.type == ExtremumType.MAX && cur.price >= prev.price) ||
                        (cur.type == ExtremumType.MIN && cur.price <= prev.price);

                if (keepCur) {
                    out.set(out.size() - 1, cur); // remplacer le précédent
                } // sinon on ignore cur
            } else {
                // Alternance respectée
                out.add(cur);
            }
        }
        return out;
    }

}


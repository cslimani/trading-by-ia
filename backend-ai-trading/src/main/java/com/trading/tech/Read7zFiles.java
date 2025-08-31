package com.trading.tech;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.IOUtils;

import com.trading.dto.Tick;

public class Read7zFiles {

	public static void main(String[] args) throws IOException {
		long starTime = System.currentTimeMillis();
        File file = new File("/data/tickdata/USATECHIDXUSD.7z");
        String market = "US100";
        // Utilisation du builder recommandé
        TickToCandles tickToCandles = new TickToCandles();
        try (SevenZFile sevenZFile =  new SevenZFile.Builder().setFile(file).get()) {
            SevenZArchiveEntry entry;

            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    System.out.println("Extraction : " + entry.getName());

                    InputStream entryStream = new InputStream() {
                        private final byte[] buf = new byte[8192];
                        private int off = 0, limit = 0;

                        @Override
                        public int read() throws IOException {
                            if (off >= limit) {
                                limit = sevenZFile.read(buf, 0, buf.length);
                                off = 0;
                                if (limit < 0) return -1;
                            }
                            return buf[off++] & 0xFF;
                        }
                    };

                    // IOUtils.lineIterator consomme ligne par ligne
                    Iterator<String> it = IOUtils.lineIterator(
                            new InputStreamReader(entryStream, StandardCharsets.UTF_8));
                    
                    Instant currentDay = null;
                    List<Tick> ticks = new ArrayList<>();
                    Tick previousTick = null;
                    while (it.hasNext()) {
                        String line = it.next();
                        Tick tick = tickToCandles.parseTick(line);
                        if (tick != null) {
                        	if (currentDay == null) {
                        		currentDay = tick.getTime().truncatedTo(ChronoUnit.DAYS);
                        	}
                        	ticks.add(tick);
                        	if (!isSameDay(currentDay, tick.getTime())) {
                        		tickToCandles.buildByTicks(market, ticks);
                        		ticks.clear();
                        		currentDay = tick.getTime().truncatedTo(ChronoUnit.DAYS);
                        		System.out.println();
                        		System.out.println("Changing day to " + currentDay);
                        		System.out.println("Previous tick time " + previousTick.getTime());
                        	}
                        	previousTick = tick;
                        }
                    }
                    System.out.println("Transforming ticks to candles");
                    
                }
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Process took " + (endTime - starTime)/1000  + " sec");
    }
	
	public static boolean isSameDay(Instant d1, Instant d2) {
	    long day1 = d1.getEpochSecond() / 86_400;
	    long day2 = d2.getEpochSecond() / 86_400;
	    return day1 == day2;
	}
}

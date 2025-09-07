package com.trading.launcher;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.trading.dto.Tick;
import com.trading.service.AbstractService;
import com.trading.tech.TickToCandles;

import lombok.extern.slf4j.Slf4j;

@Component
@Profile("insert-candles")
@Slf4j
public class InsertCandlesInDatabase extends AbstractService implements CommandLineRunner {


	@Autowired
	TickToCandles tickToCandles;

	ExecutorService executor = Executors.newFixedThreadPool(5);

	@Override
	public void run(String... args) throws Exception {
		long starTime = System.currentTimeMillis();
		List.of("GOLD").forEach(market -> {
			System.out.println("Processing " + market);
			File file = new File(String.format("/data/tickdata/%s.7z", market));
			if (!file.exists()) {
				System.out.println("File does not exist");
				return;
			}
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
						List<Tick> ticks = new ArrayList<>(1000_000);
						while (it.hasNext()) {
							String line = it.next();
							Tick tick = tickToCandles.parseTick(line);
							if (tick != null) {
								if (currentDay == null) {
									currentDay = tick.getTime().truncatedTo(ChronoUnit.DAYS);
								}
								if (!isSameDay(currentDay, tick.getTime())) {
									List<Tick> ticksCopy = new ArrayList<>(ticks);
									executor.submit(() -> {
										try {
											tickToCandles.buildByTicks(market, new ArrayList<Tick>(ticksCopy));
										} catch (Exception e) {
											log.error("Error", e);
										}
									});
									ticks.clear();
									currentDay = tick.getTime().truncatedTo(ChronoUnit.DAYS);
//									System.out.println();
//									System.out.println("Changing day to " + currentDay);
								}
								ticks.add(tick);
							}
						}
						tickToCandles.buildByTicks(market, ticks);
						System.out.println("Transforming ticks to candles");
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});
		long endTime = System.currentTimeMillis();
		executor.shutdown();
		if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
			executor.shutdownNow();
		}
		System.out.println("Process took " + (endTime - starTime)/1000  + " sec");
	}

	public static boolean isSameDay(Instant d1, Instant d2) {
		long day1 = d1.getEpochSecond() / 86_400;
		long day2 = d2.getEpochSecond() / 86_400;
		return day1 == day2;
	}

}

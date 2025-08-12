package com.trading.feature;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.trading.component.ZoneSelectedListener;
import com.trading.dto.HorizontalLine;
import com.trading.dto.PeriodOfTime;
import com.trading.entity.Candel;
import com.trading.entity.Market;
import com.trading.entity.TrainablePattern;
import com.trading.entity.TrainingTrade;
import com.trading.enums.EnumDirection;
import com.trading.enums.EnumTimeRange;
import com.trading.utils.DateHelper;

import jakarta.transaction.Transactional;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

@Component
public class PriceAnalysisFeature extends AbstractFeature implements ZoneSelectedListener{

	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final int MAX_CANDELS = 1000;
	//	Map<EnumTimeRange, PeriodOfTime> map = new HashMap<>();
	private List<Candel> candels;
	private LocalDateTime dateEnd;
	private LocalDateTime dateStart = LocalDateTime.of(2015, 1, 1, 0, 0);

	public void init() {
		Optional<TrainingTrade> latestTrade = trainingTradeRepository.findFirstByMarketOrderByDateCloseDesc(data.getMarket().getCode());
		if (latestTrade.isPresent()) {
			TrainingTrade trade = latestTrade.get();
			System.out.println("Last trade closed at " + trade.getDateClose());
			dateEnd = trade.getDateClose();
		}
		dateStart = dateEnd.plusHours(-100);
		Market market = data.getMarket();
		List<Candel> candelsTmp = candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(market.getCode(),EnumTimeRange.H1, dateStart, dateEnd);
		int nbTry = 0;
		while (candelsTmp.size() < 100) {
			dateStart = dateStart.plusHours(-20);
			candelsTmp = candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateAsc(market.getCode(), EnumTimeRange.H1, dateStart, dateEnd);
			if (nbTry++ > 10) {
				throw new IllegalArgumentException("No candel found after 10 tries");
			}
		}

//		addButton("Send", () -> sendForAnalysis(true), false);
//		addButton("Backtest", () -> backtest(), false);
//		addButton("<<", () -> previous(), true);
//		addButton(">>", () -> next(), true);
		addButton("Delete", () -> deleteBOS(), true);
//		addButton("KO", () -> processed(-1f), true);
		addButton("Stretch Zone", () -> stretchZone(30), true);
		addButton("Capture Zone", () -> captureZone(), true);
//		addButton("Switch", () -> switchLevel(), true, true);
		addButton("-50", () -> shiftBackwardBy(50), true);
		addButton("-10", () -> shiftBackwardBy(10), true);
		addButton("-1", () -> shiftBackwardBy(1), true);
		addButton("+1", () -> shiftForwardBy(1), true);
		addButton("+10", () -> shiftForwardBy(10), true);
		addButton("+50", () -> shiftForwardBy(50), true);
		zoneSelectorComponent.addListener(this);
		loadPattern();
	}



//	private void backtest() {
//		TrainablePattern pattern = trainablePatternComponent.getPattern();
//		pattern.getDateEnd()
//	}



	private void sendForAnalysis(boolean async) {
		TrainablePattern pattern = trainablePatternComponent.getPattern();
		System.out.println(data.getCandels().get(0).getDate());
		HttpUrl.Builder httpBuilder = HttpUrl.parse("http://localhost:8080/check").newBuilder();
		httpBuilder.addQueryParameter("dateStart", pattern.getDateStart().format(formatter));
		httpBuilder.addQueryParameter("dateEnd", pattern.getDateEnd().format(formatter));

		Request request = new Request.Builder().url(httpBuilder.build()).build();
		if (async) {
			client.newCall(request).enqueue(new Callback() {
						@Override
						public void onResponse(Call call, Response response) throws IOException {
						}
			
						@Override
						public void onFailure(Call call, IOException e) {
						}
					});
		} else {
			try {
				String body = client.newCall(request).execute().body().string();
				if (body.length() > 0) {
					LocalDateTime date = LocalDateTime.parse(body, formatter);
					candels.stream().filter(c -> c.getDate().isEqual(date)).forEach(c -> c.setColor(Color.WHITE));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}



	public void loadData() {
		EnumTimeRange timeRange = data.getTimeRange();
		levelService.saveTimerange(timeRange);
		PeriodOfTime period = levelService.getPeriodOfTime();
		if (period == null) {
			System.out.println("*******************************************");
			System.out.println("Warning no pattern loaded, set default date");
			System.out.println("*******************************************");
			levelService.savePeriod(dateStart, dateEnd);
			period = levelService.getPeriodOfTime();
		}
		if (data.getDirection() == null) {
			data.setDirection(EnumDirection.BUY.name());
		}
		candels = candelRepository.findByMarketAndTimeRangeAndDateBetweenOrderByDateDesc(
				data.getMarket().getCode(),
				timeRange, period.getDateStart(),
				period.getDateEnd(),
				PageRequest.of(0, MAX_CANDELS));
		Collections.reverse(candels);
		topPanel.setNbCandels(candels.size());
		backtestFeature.onLoadData(candels);
//		sendForAnalysis(false);
		TrainablePattern pattern = trainablePatternComponent.getPattern();
		if (pattern != null && pattern.getContent() != null && pattern.getContent().getDatesToColor() != null) {
			candels.stream().filter(c -> pattern.getContent().getDatesToColor().contains(c.getDate())).forEach(c -> c.setColor(Color.WHITE));
		}
		if (data.getTrainingTrades() != null) {
			data.getTrainingTrades().forEach(trade -> {
				candels.stream().filter(c -> {
//					System.out.println(DateHelper.getTruncatedDate(trade.getDateOpen(), timeRange));
					return  DateHelper.getTruncatedDate(trade.getDateOpen(), timeRange).isEqual(DateHelper.getTruncatedDate(c.getDate(), timeRange));	
				}).forEach(c -> c.setColor(Color.WHITE));
			});
		}
		//		if (candels.isEmpty()) {
		//			System.out.println();
		//		}
		//		System.out.println("Candels loaded from " + candels.get(0).getDate() + " to " + Iterables.getLast(candels).getDate() + " found => " + candels.size());
	}

	private void loadPattern() {
		TrainablePattern pattern = trainablePatternComponent.getPattern();
		if (pattern != null) {
			data.setDirection(pattern.getDirection());
			data.getPricesToDraw().clear();
			data.setMarket(marketRepository.getByCode(pattern.getMarket()));
			if (pattern.getTradeId() != null) {
//				TrainingTrade trade = trainingTradeRepository.findByIdentifier(pattern.getTradeId());
//				data.getPricesToDraw().add(new HorizontalLine(trade.getStoploss(), Color.RED, "ZONE"));
//				pattern.getContent().setDateEnterZone(trade.getDateOpen());
//				pattern.getContent().setDatesToColor(List.of(trade.getDateOpen()));
			}
			//			System.out.println(pattern.getDateStart() + " " + pattern.getDateEnd());
			levelService.load(pattern);
			topPanel.setTimeRange(levelService.getTimerange());
			topPanel.setDateAndPrice(pattern.getDateStart(), pattern.getPriceEntryZone());
			data.setTimeRange(levelService.getTimerange());
			data.setSource(pattern.getSource());
			topPanel.setColoredPanel(pattern.getSentiment());

			if (pattern.getContent() != null) {
				if (pattern.getContent().getPriceEnterZone() != null) {
					data.getPricesToDraw().add(new HorizontalLine(pattern.getContent().getPriceEnterZone(), Color.YELLOW, "ZONE"));
				}
			}
			
			if (pattern.getRiskRatio() != null) {
				topPanel.showDiff(pattern.getRiskRatio(), 0d);
				System.out.println("RR = " + pattern.getRiskRatio());
			}
		} 
	}

	private void switchLevel() {
		levelService.switchLevel();
		data.setTimeRange(levelService.getTimerange());
		topPanel.setTimeRange(levelService.getTimerange());
	}

	private void previous() {
		trainablePatternComponent.previous();
		loadPattern();
		mainPanel.unzoomMaximum();
	}

	private void deleteBOS() {
		trainablePatternComponent.delete();
		next();
	}

	@Transactional
	private void next() {
		trainablePatternComponent.next();
		backtestFeature.reset();
		loadPattern();
		mainPanel.unzoomMaximum();
	}

	private void stretchZone(int value) {
		EnumTimeRange timeRange = data.getTimeRange();
		PeriodOfTime period = levelService.getPeriodOfTime();
		Pageable page = PageRequest.of(0, value);
		List<Candel> candels = candelRepository.findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(data.getMarket().getCode() ,timeRange, period.getDateStart(), page);
		levelService.savePeriod(Iterables.getLast(candels).getDate(), period.getDateEnd());
	}

	private void captureZone() {
		levelService.savePeriod(data.getDateStart(), data.getDateEnd());
	}

	private void shiftBackwardBy(int nbShift) {
		EnumTimeRange timeRange = data.getTimeRange();
		PeriodOfTime period = levelService.getPeriodOfTime();
		Pageable page = PageRequest.of(0, nbShift);
		List<Candel> nextCandels = candelRepository.findByMarketAndTimeRangeAndDateBeforeOrderByDateDesc(data.getMarket().getCode() ,timeRange, period.getDateEnd(), page);
		levelService.saveDateEnd(Iterables.getLast(nextCandels).getDate());
	}

	private void shiftForwardBy(int nbShift) {
		EnumTimeRange timeRange = data.getTimeRange();
		PeriodOfTime period = levelService.getPeriodOfTime();
		Pageable page = PageRequest.of(0, nbShift);
		List<Candel> nextCandels = candelRepository.findByMarketAndTimeRangeAndDateAfterOrderByDateAsc(data.getMarket().getCode(), timeRange, period.getDateEnd(), page);
		levelService.saveDateEnd(Iterables.getLast(nextCandels).getDate());
		backtestFeature.addCandels(nextCandels);
	}


	public List<Candel> getCandels() {
		return candels;
	}

	@Override
	public void onNewZoneSelected(LocalDateTime dateStart, LocalDateTime dateEnd) {
		levelService.savePeriod(dateStart, dateEnd);
		mainPanel.reload();
		mainPanel.unzoomMaximum();
	}

	@Override
	public void onNewPointSelected(LocalDateTime date) {
		levelService.saveDateEnd(date);
		mainPanel.reload();
		//		mainPanel.unzoomMaximum();
	}

	public void keyReleased(int keyCode) {
		if (keyCode == 83) {
			switchLevel();
		}
	}

	public void onCloseTrade(TrainingTrade trade) {
		trainablePatternComponent.onCloseTrade(trade);
		next();
	}

	private void processed(Double rr) {
		trainablePatternComponent.processed(rr);
		next();
	}

}

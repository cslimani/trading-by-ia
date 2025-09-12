package com.trading.gui;

import java.time.LocalDateTime;

import javax.swing.JPanel;

import org.springframework.beans.factory.annotation.Autowired;

import com.trading.component.DistanceFeature;
import com.trading.component.HorizontalLineFeature;
import com.trading.component.IndicatorFeature;
import com.trading.component.TradeFeature;
import com.trading.component.VerticalLineFeature;
import com.trading.component.ZoneSelectorFeature;
import com.trading.dto.DataDTO;
import com.trading.dto.GraphDateDTO;
import com.trading.entity.Candle;
import com.trading.repository.MarketRepository;
import com.trading.runner.Runner;
import com.trading.service.PriceService;

public class AbstractPanel extends JPanel{

	private static final long serialVersionUID = 1L;

	@Autowired
	public DataDTO data;
	@Autowired
	protected PriceService service; 
	@Autowired
	protected MarketRepository marketDAO;
	@Autowired
	ZoneSelectorFeature zoneSelectorComponent;
	@Autowired
	HorizontalLineFeature horizontalLineComponent;
	@Autowired
	VerticalLineFeature verticalLineComponent;
	@Autowired
	protected Runner runner;
	@Autowired
	IndicatorFeature indicatorFeature;
	@Autowired
	TradeFeature riskRatioFeature;
	@Autowired
	DistanceFeature distanceFeature;
	
	protected Double pixelToPrice(int pixel) {
		return data.getPriceMax() - pixel*data.getGapPrice()/getHeight()*1f;
	}

	protected Double absolutePixelToPrice(int pixel) {
		return pixel*data.getGapPrice()/getHeight()*1f;
	}

	protected void prepareData() {
		synchronized (MainPanel.LOCK) {
			data.setPriceMax(null);
			data.setPriceMin(null);
			LocalDateTime dateStart = getDateFromPixel(data.getViewPosition());
			LocalDateTime dateEnd = getDateFromPixel(data.getViewPosition() + data.getViewWidth());
			if (dateEnd == null) {
				dateEnd = data.getLastGraphDate().getDate();
			}
			if (dateStart == null) {
				dateStart = data.getGraphDates().get(0).getDate();
			}
			for (Candle candle : data.getCandles()) {
				candle.setDraw(false);
				if ((candle.getDate().isAfter(dateStart) || candle.getDate().equals(dateStart)) && 
						(candle.getDate().isBefore(dateEnd) || candle.getDate().equals(dateEnd)) ) {
					if (data.getPriceMax() == null || candle.getHigh() > data.getPriceMax()) {
						data.setPriceMax(candle.getHigh());
					}
					if (data.getPriceMin() == null || candle.getLow() < data.getPriceMin()) {
						data.setPriceMin(candle.getLow());
					}

					candle.setDraw(true);
				}
			}
			if (data.getPriceMax() != null && data.getPriceMin() != null) {
				Double gapTmp = data.getPriceMax() - data.getPriceMin();
				data.setPriceMax(data.getPriceMax() + gapTmp*0.05f);
				data.setPriceMin(data.getPriceMin() - gapTmp*0.05f);
				data.setGapPrice(data.getPriceMax() - data.getPriceMin());
				data.setDateStart(dateStart);
				data.setDateEnd(dateEnd);
			}
		}
	}

	protected LocalDateTime getDateFromPixel(int pixelPosition) {
		for (GraphDateDTO graphDateDTO : data.getGraphDates()) {
			if (pixelPosition >= graphDateDTO.getStartPosition()   && pixelPosition <= graphDateDTO.getEndPosition()) {
				return graphDateDTO.getDate();
			}
		}
		return null;
	}


}

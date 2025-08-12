package com.trading.gui;

import java.time.LocalDateTime;

import javax.swing.JPanel;

import org.springframework.beans.factory.annotation.Autowired;

import com.trading.component.AlertComponent;
import com.trading.component.BacktestComponent;
import com.trading.component.HorizontalLineComponent;
import com.trading.component.RiskRatioComponent;
import com.trading.component.VerticalLineComponent;
import com.trading.component.ZoneSelectorComponent;
import com.trading.dto.DataDTO;
import com.trading.dto.GraphDateDTO;
import com.trading.entity.Candel;
import com.trading.feature.AccumulationFeature;
import com.trading.feature.RiskRatioFeature;
import com.trading.feature.TrainablePatternComponent;
import com.trading.repository.MarketRepository;
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
	RiskRatioFeature riskRatioFeature;
	@Autowired
	ZoneSelectorComponent zoneSelectorComponent;
	@Autowired
	TrainablePatternComponent trainablePatternComponent;
	@Autowired
	HorizontalLineComponent horizontalLineComponent;
	@Autowired
	VerticalLineComponent verticalLineComponent;
	@Autowired
	RiskRatioComponent counterComponent;
	@Autowired
	AlertComponent alertComponent;
	@Autowired
	AccumulationFeature accumulationFeature;
	@Autowired
	BacktestComponent backtestComponent;
	
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
			for (Candel candel : data.getCandels()) {
				candel.setDraw(false);
				if ((candel.getDate().isAfter(dateStart) || candel.getDate().equals(dateStart)) && 
						(candel.getDate().isBefore(dateEnd) || candel.getDate().equals(dateEnd)) ) {
					if (data.getPriceMax() == null || candel.getHigh() > data.getPriceMax()) {
						data.setPriceMax(candel.getHigh());
					}
					if (data.getPriceMin() == null || candel.getLow() < data.getPriceMin()) {
						data.setPriceMin(candel.getLow());
					}

					candel.setDraw(true);
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

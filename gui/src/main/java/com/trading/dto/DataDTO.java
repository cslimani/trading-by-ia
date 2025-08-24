package com.trading.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

import com.trading.entity.Candle;
import com.trading.entity.Market;
import com.trading.enums.EnumTimeRange;

import lombok.Data;

@Component
@Data
public class DataDTO {

	private Double priceMax;
	private Double priceMin;
	private Double gapPrice;
	private long gapSeconds;
	private List<Candle> candles;
	private Map<LocalDateTime, Candle> mapCandles = new HashMap<>();
	private List<GraphDateDTO> graphDates;
	private Market market;
	private String marketCode;
	private int candleWidth;
	private int spaceBetweenCandles;
	private int position;
	private List<GuiLineDTO> lines = new ArrayList<GuiLineDTO>();
	private EnumTimeRange timeRange;
	private int shift = 0;
	private int finalShift = 0;
	private GraphDateDTO lastGraphDate;
	private GraphDateDTO firstGraphDate;
	private int viewPosition=0;
	private int viewWidth=0;
	private int viewTotalWidth=0;
	private int viewHeight=0;
	private int nbCandlesAdded=0;
	private List<HorizontalLine> pricesToDraw = new CopyOnWriteArrayList<>();
	int indexTrade = 0;
	int index = 0;
	LocalDateTime dateEnd;
	LocalDateTime dateStart;
	boolean autoZoomDone = false;
	String tradeType = "WICKOFF";
	String source;
	String direction;
	Map<String, Market> mapMarkets = new ConcurrentHashMap<String, Market>();
	List<VerticalLine> verticalLines = new ArrayList<VerticalLine>();
	
	public void init(int candleWidth, int spaceBetweenCandles) {
		this.candleWidth = candleWidth;
		this.position = 0;
		this.spaceBetweenCandles = spaceBetweenCandles;
	}

//	public boolean isBOSMode() {
//		return mode == EnumMode.BREAK_OF_STRUCTURE;
//	}
//
//	public boolean isTrainingTradeMode() {
//		return mode == EnumMode.TRAINING_TRADE;
//	}
//
//	public boolean isTradeVisualisation() {
//		return mode == EnumMode.TRADE_VISUALIZATION;
//	}
	
}

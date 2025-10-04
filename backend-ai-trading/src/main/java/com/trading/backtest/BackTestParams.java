package com.trading.backtest;

import lombok.Data;

@Data
public class BackTestParams {


	public Double maxRangeAtrRatioLow;
	public Double maxRangeAtrRatioHigh;
	public Double minRangeAtrRatio;
	public Double bottomBandRatio;
	public Double topBandRatio;
	public Double breakDownAtrRatio;
	public Double breakUpAtrRatio;
	public Integer nbBottomsRequired;
	public Integer minRangeWidth;
	public Integer nbCandlesBefore;
	public Integer maxRangeWidth;

}

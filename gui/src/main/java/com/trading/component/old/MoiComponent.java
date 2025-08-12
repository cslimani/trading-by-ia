package com.trading.component.old;

import org.springframework.stereotype.Component;

import com.trading.dto.PeriodOfTime;
import com.trading.entity.MomentOfInterest;
import com.trading.enums.EnumTimeRange;
import com.trading.feature.AbstractFeature;

@Component
public class MoiComponent extends AbstractFeature{

	private MomentOfInterest moi;

	public void nextMOI() {
		if (moi != null) {
			loadMOI(moiRespository.findFirstByMarketAndDateEndAfterOrderByDateEndAsc(data.getMarket().getCode(), moi.getDateEnd()));
		}
	}

	public void previousMOI() {
		if (moi != null) {
			loadMOI(moiRespository.findFirstByMarketAndDateEndBeforeOrderByDateEndDesc(data.getMarket().getCode(), moi.getDateEnd()));
		}
	}

	public void deleteMOI() {
		if (moi != null) {
			transactionnal(() -> {
				MomentOfInterest moiToDelete = moiRespository.save(moi);
				moiRespository.delete(moiToDelete);
				moi = null;
			});
		}
	}

	private void loadLastMOI() {
		loadMOI(moiRespository.findFirstByMarketOrderByDateEndDesc(data.getMarket().getCode()));
	}

	private void loadMOI(MomentOfInterest moiFound) {
		if (moiFound != null) {
			moi = moiFound;
			PeriodOfTime period = new PeriodOfTime(moi.getDateStart(), moi.getDateEnd());
//			map.clear();
//			map.put(EnumTimeRange.H1, period);
			data.setTimeRange(moi.getTimeRange());
			topPanel.setTimeRange(moi.getTimeRange());
		}
	}

	public void saveMOI(PeriodOfTime period) {
		EnumTimeRange timeRange = data.getTimeRange();
		moi = new MomentOfInterest();
		moi.setDateStart(period.getDateStart());
		moi.setDateEnd(period.getDateEnd());
		moi.setTimeRange(timeRange);
		moi.setMarket(data.getMarket().getCode());
		moiRespository.save(moi);
	}
}

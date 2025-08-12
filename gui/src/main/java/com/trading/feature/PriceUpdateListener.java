package com.trading.feature;

import com.trading.entity.Candel;
import com.trading.enums.EnumTimeRange;

public interface PriceUpdateListener {

	void onUpdatePrice(Candel candel, EnumTimeRange tr);

}

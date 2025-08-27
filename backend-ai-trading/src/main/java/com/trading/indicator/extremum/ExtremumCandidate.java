package com.trading.indicator.extremum;

import com.trading.entity.Candle;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ExtremumCandidate {

	@NonNull
	Candle candle;
	@NonNull
	Integer thresold;
	Integer okFollowing = 0;
	
	public double getMax() {
		return candle.getHigh();
	}

	public double getMin() {
		return candle.getLow();
	}

	public boolean isValid() {
		return okFollowing >= thresold;
	}

	public void increaseFollowing() {
		okFollowing++;		
	}

	public boolean processMax(Candle c) {
		if (c.getHigh() > getMax()) {
			reset(c);
			return false;
		} else {
			okFollowing++;
			return true;
		}
	}

	public boolean processMin(Candle c) {
		if (c.getLow() < getMin()) {
			reset(c);
			return false;
		} else {
			okFollowing++;
			return true;
		}
	}

	public void reset(Candle c) {
		candle = c;
		okFollowing = 0;
	}
	
}

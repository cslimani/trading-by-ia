package com.trading.dto;

import com.trading.entity.Candel;

import lombok.Getter;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

@Getter
public class LightCandel extends SelfDescribingMarshallable{

	Double open;
	Double close;
	Double high;
	Double low;
	Integer hour;
	Integer year;
	String dateString;
	Double lowBid;
	Double highBid;
	Double openBid;
	Double closeBid;
	
	public Candel toCandel() {
		Candel candel = new Candel();
//		candel.set
//		candel.set
//		candel.set
//		candel.set
//		candel.set
//		candel.set
//		candel.set
//		candel.set
//		candel.set
//		candel.set
		return candel;
	}
	
	
}

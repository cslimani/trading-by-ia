package com.trading.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trading.entity.Candel;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class PriceLine {
	
	@NonNull
	String market;
	@NonNull
	@JsonIgnore
	String prices;
	@NonNull
	List<Candel> rlPreviousPriceList;
	@NonNull
	List<Candel> rlFuturPriceList;
	@NonNull
	Double spread;
	@NonNull
	LocalDateTime date;
	@NonNull
	String direction;
	Double prediction;
	
}

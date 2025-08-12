package com.trading.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaCandelDTO {

	Instant time;
	Long tickVolume;
	Double low;
	Double high;
	Double open;
	Double close;
}

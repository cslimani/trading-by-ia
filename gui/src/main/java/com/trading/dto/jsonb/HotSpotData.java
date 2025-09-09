package com.trading.dto.jsonb;

import java.util.List;

import com.trading.dto.DatePoint;
import com.trading.dto.HorizontalLine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotSpotData {

	List<HorizontalLine> lines;
	List<String> labels;
	List<DatePoint> points;
}

package com.trading.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.trading.dto.PeriodOfTime;
import com.trading.entity.TrainablePattern;
import com.trading.enums.EnumLevel;
import com.trading.enums.EnumTimeRange;

@Service
public class LevelService {
	
//	@Autowired
//	public DataDTO data;
	
	EnumLevel level = EnumLevel.HIGH;
	Map<EnumLevel, LocalDateTime> mapDateStart = new HashMap<>();
	Map<EnumLevel, EnumTimeRange> mapTimerange = new HashMap<>();
	LocalDateTime dateEnd;
	
	public void savePeriod(LocalDateTime dateStart, LocalDateTime dateEnd) {
		this.dateEnd = dateEnd;
		mapDateStart.put(level, dateStart);
	}
	public void saveDateEnd(LocalDateTime dateEnd) {
		this.dateEnd = dateEnd;
	}
	
	public PeriodOfTime getPeriodOfTime() {
		if (mapDateStart.get(level) != null && dateEnd != null) {
			return new PeriodOfTime(mapDateStart.get(level), dateEnd);
		}
		return null;
	}
	
	public void load(TrainablePattern pattern) {
		mapDateStart.put(EnumLevel.LOW, pattern.getDateStart());
		mapTimerange.put(EnumLevel.LOW, pattern.getTimeRange());
		
		if (pattern.getContent() != null && pattern.getContent().getDateParentEnd() != null && 
				pattern.getContent().getDateParentStart() != null) {
			mapDateStart.put(EnumLevel.HIGH, pattern.getContent().getDateParentStart().minusDays(2));
//			pattern.getContent().setDatesToColor(List.of(pattern.getContent().getDateParentStart()));
			this.dateEnd = pattern.getContent().getDateParentEnd();
			mapTimerange.put(EnumLevel.HIGH, pattern.getContent().getTimerange());
		} else {
			this.dateEnd = pattern.getDateEnd().plusMinutes(30);
		}
		
	}
	
	public void switchLevel() {
		level = level == EnumLevel.HIGH ? EnumLevel.LOW : EnumLevel.HIGH;
	}
	public EnumTimeRange getTimerange() {
		return mapTimerange.get(level);
	}
	public void saveTimerange(EnumTimeRange timeRange) {
		mapTimerange.put(level, timeRange);
	}
	
}

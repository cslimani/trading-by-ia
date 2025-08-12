package com.trading.metatrader;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class MetaServer {

	@NonNull
	private String url;
	private LocalDateTime lastDateError;
	private AtomicInteger nbConcurrent = new AtomicInteger(0);
	
	public void acquire() {
		nbConcurrent.incrementAndGet();		
	}
	
	public void release() {
		nbConcurrent.decrementAndGet();		
	}
	
	public Integer getConcurrentCalls() {
		return nbConcurrent.get();	
	}
	
	public void error() {
		lastDateError = LocalDateTime.now();
	}
}

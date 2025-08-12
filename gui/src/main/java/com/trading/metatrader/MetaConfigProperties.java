package com.trading.metatrader;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "meta")
@Data
public class MetaConfigProperties {

	private String token;
	private List<String> servers;
	
}

package com.trading.metatrader;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ServerManager {

	private static final int MAX_CONCURRENT_CALLS = 3;
	private static Logger SERVER_LOGGER = LoggerFactory.getLogger("SERVER_LOGGER");
	@Autowired
	MetaConfigProperties metaConfigProperties ;
	List<MetaServer> servers;

	@PostConstruct
	public void init() {
		servers = metaConfigProperties.getServers().stream().map(url -> new MetaServer(url)).toList();
	}

	public synchronized MetaServer getServer() {
		MetaServer server = null;
		//Errors
		List<MetaServer> serversWithNoError = servers.stream().filter(s -> {
			return s.getLastDateError() == null || s.getLastDateError().isBefore(LocalDateTime.now().minusMinutes(1));
		}).toList();
		if (serversWithNoError.isEmpty()) {
			log.error("No available server with no error, using the whole list");
			serversWithNoError = servers;
		}
		servers.stream().filter(s -> {
			return s.getLastDateError() != null && s.getLastDateError().isAfter(LocalDateTime.now().minusMinutes(1));
		}).forEach(s -> log.warn("Server {} is on error, will be available after", s.getUrl(), s.getLastDateError().plusMinutes(1)));
		
		//Busy 
		while (server == null) {
			List<MetaServer> serversFree = serversWithNoError.stream().filter(s -> {
				return s.getNbConcurrent().get() < MAX_CONCURRENT_CALLS;
			}).sorted(Comparator.comparing(MetaServer::getConcurrentCalls)).toList();
			if (!serversFree.isEmpty()) {
				server = serversFree.get(0);
			} else {
				SERVER_LOGGER.info("No available server free, we have to wait to get one");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					log.error("Error waiting for a free server", e);
				}
			}
		}
		SERVER_LOGGER.info("Server found concurrent calls before use is {} for {}", server.getNbConcurrent().get(), server.getUrl());
		server.acquire();
		return server;
	}

}

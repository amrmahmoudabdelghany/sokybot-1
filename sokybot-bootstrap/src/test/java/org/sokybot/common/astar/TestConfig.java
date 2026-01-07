package org.sokybot.common.astar;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class TestConfig {

	
	
	@Bean
	CacheManager cacheManager() { 
		return new ConcurrentMapCacheManager() ;
	}
	
	@Bean
	Executor executor() { 
		return Executors.newCachedThreadPool() ; 
	}
}

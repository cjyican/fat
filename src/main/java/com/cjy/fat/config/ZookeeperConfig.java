package com.cjy.fat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name="fat.zookeeper.host")
public class ZookeeperConfig {
	
	public static Logger LOG  = LoggerFactory.getLogger(ZookeeperConfig.class);
	
	@Value("${fat.zookeeper.host:}")
	private String host;
	
	@Value("${fat.zookeeper.sessionTimeout:10000}")
	private int sessionTimeout;
	
	@Bean
	public ZooTemplate zooTemplate() throws Exception{
		
		LOG.info("use zookeeper as register...");
		
		return new ZooTemplate(host, sessionTimeout);
		
	}
	
}

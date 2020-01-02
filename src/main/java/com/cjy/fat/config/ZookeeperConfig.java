package com.cjy.fat.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZookeeperConfig {
	
	public static Logger LOG  = LoggerFactory.getLogger(ZookeeperConfig.class);
	
	@Value("${fat.zookeeper.host:}")
	private String host;
	
	@Value("${fat.zookeeper.sessionTimeout:60000}")
	private int sessionTimeout;
	
	@Bean
	public ZooTemplate zooTemplate() throws Exception{
		
		if(StringUtils.isBlank(host)) {
			return null;
		}
		
		return new ZooTemplate(host, sessionTimeout);
		
	}
	
}

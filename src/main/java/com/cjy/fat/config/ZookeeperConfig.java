package com.cjy.fat.config;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZookeeperConfig {

	@Value("${fat.zookeeper.host}")
	private String host;
	
	@Value("${fat.zookeeper.sessionTimeout:60000}")
	private int sessionTimeout;
	
	@PostConstruct
	public void initZooTemplate() throws Exception{
		
		if(StringUtils.isBlank(host)) {
			return ;
		}
		
		ZooTemplate.initZooTemplate(host, sessionTimeout);
		
	}
	
}

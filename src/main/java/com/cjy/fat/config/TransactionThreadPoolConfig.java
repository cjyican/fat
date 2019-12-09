package com.cjy.fat.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TransactionThreadPoolConfig {
	
	@Value("${fat.thread.core_pool_size:20}")
	private int corePoolSize ;
	
	@Value("${fat.thread.max_pool_size:50}")
	private int maxPoolSize ;
	
	@Value("${fat.thread.queue_capacity:200}")
	private int queueCapacity ;
	
	@Value("${fat.thread.keep_alive_seconds:60}")
	private int keepAliveSeconds;

	@Bean
	public AsyncTaskExecutor transactionResolveExecutor() {
		 ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
         executor.setCorePoolSize(corePoolSize);//初始化线程数
         executor.setMaxPoolSize(maxPoolSize);//最大线程数量
         executor.setQueueCapacity(queueCapacity);//缓冲数量
         executor.setKeepAliveSeconds(keepAliveSeconds);//60秒内该线程未被执行自动销毁
         executor.setThreadNamePrefix("fat_resolve-");
         executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
         executor.initialize();
         return executor;
	}

}

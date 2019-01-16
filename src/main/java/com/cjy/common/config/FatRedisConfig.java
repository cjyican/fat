package com.cjy.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class FatRedisConfig {

	@Value("${fat.redis.host}")
	private String host;

	@Value("${fat.redis.database:0}")
	private int database;

	@Value("${fat.redis.port}")
	private int port;

	@Value("${fat.redis.password}")
	private String password;

	@Value("${fat.redis.timeout:-1}")
	private int timeout;

	@Value("${fat.redis.pool.max-active:20}")
	private int maxActive;

	@Value("${fat.redis.pool.max-wait:3000}")
	private int maxWait;

	@Value("${fat.redis.pool.max-idle:10}")
	private int maxIdle;

	@Value("${fat.redis.pool.min-idle:5}")
	private int minIdle;
	
	@Bean
	public RedisTemplate<String, Object> fatRedis() { 
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>(); 
		redisTemplate.setConnectionFactory(this.connectionFactory());
		StringRedisSerializer stringRedisSerializer =new StringRedisSerializer(); 
		redisTemplate.setValueSerializer(stringRedisSerializer); 
		redisTemplate.setKeySerializer(stringRedisSerializer); 
		redisTemplate.setHashKeySerializer(stringRedisSerializer); 
		redisTemplate.setHashValueSerializer(stringRedisSerializer);
		return redisTemplate; 
	}

	public RedisConnectionFactory connectionFactory() {
		JedisConnectionFactory jedis = new JedisConnectionFactory();
		jedis.setHostName(host);
		jedis.setPort(port);
		jedis.setPassword(password);
		jedis.setDatabase(database);
		jedis.setTimeout(timeout);
		jedis.setPoolConfig(this.poolCofig()); // 初始化连接pool
		jedis.afterPropertiesSet();
		RedisConnectionFactory factory = jedis;
		return factory;
	}

	public JedisPoolConfig poolCofig() {
		JedisPoolConfig poolCofig = new JedisPoolConfig();
		poolCofig.setMaxIdle(Integer.valueOf(maxIdle));
		poolCofig.setMaxTotal(maxActive);
		poolCofig.setMaxWaitMillis(maxWait);
		poolCofig.setMinIdle(minIdle);
		return poolCofig;
	}

}

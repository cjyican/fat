package com.cjy.fat.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.cjy.fat.util.CollectionUtil;

import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

	@Value("${fat.redis.host:127.0.0.1}")
	private String host;

	@Value("${fat.redis.database:0}")
	private int database;

	@Value("${fat.redis.port:6379}")
	private int port;

	@Value("${fat.redis.password:}")
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
	
	@Value("${fat.redis.cluster.nodes:}")
	private String clusterNodes;
	
	@Value("${fat.redis.cluster.max-redirects:0}")
	private int maxRedirects;
	
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
		JedisConnectionFactory jedis = null;
		RedisClusterConfiguration clusterConfig = buildRedisClusterConfig();
		if(null != clusterConfig) {
			jedis = new JedisConnectionFactory(clusterConfig);
		}else{
			jedis = new JedisConnectionFactory();
		}
		jedis.setHostName(host);
		jedis.setPort(port);
		if(StringUtils.isNotBlank(StringUtils.trim(password))) {
			jedis.setPassword(password);
		}
		jedis.setDatabase(database);
		jedis.setTimeout(timeout);
		jedis.setPoolConfig(this.poolCofig()); // 初始化连接pool
		jedis.afterPropertiesSet();
		RedisConnectionFactory factory = jedis;
		return factory;
	}
	
	public RedisClusterConfiguration buildRedisClusterConfig() {
		if(StringUtils.isNotBlank(StringUtils.trim(clusterNodes))) {
			RedisClusterConfiguration clusterConfigeration = new RedisClusterConfiguration(CollectionUtil.covertStringToCollection(clusterNodes));
			if(maxRedirects == 0){
				maxRedirects = clusterConfigeration.getClusterNodes().size();
			}
			clusterConfigeration.setMaxRedirects(maxRedirects);
			return clusterConfigeration;
		}
		return null;
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

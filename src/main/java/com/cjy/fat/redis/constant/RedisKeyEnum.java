package com.cjy.fat.redis.constant;

import com.cjy.fat.redis.RedisHelper;

public enum RedisKeyEnum {
	
	/**
	 * fat前缀
	 */
	FAT_PRE("fat:"),
	
	/**
	 * 事务标识 ， 本地UUID + redis自增序列
	 */
	FAT_KEY_ID("fat_id"),
	
	/**
	 * 服务标识,SET
	 */
	SERVICE_ID_SET(":service_id:"),
	
	/**
	 * 是否有服务/事务出错 {@link RedisHelper }
	 */
	IS_SERVICE_ERROR(":service_error:"),
	
	/**
	 * 加入的服务/事务列表 ， left push
	 */
	SERVICE_SET(":service_set:"),
	
	/**
	 * 事务分组协调器完成的列表，set
	 */
	GROUP_FINISH_ZSET(":group:finish_zset:"),
	
	/**
	 * 是服务分组协调器完成标识 ， 阻塞队列
	 */
	GROUP_CANCOMMIT_LIST(":group:cancommit_list:"),
	
	/**
	 * 该事务分组协调器管理的用于阻塞所有事务的元素
	 */
	GROUP_SERVICE_SET(":group:service_set:"),
	
	/**
	 * 是否预备（所有服务已经完成业务处理）标识，当第一个服务成功成阻塞队列中获取到元素的时候，该标识赋值为0，后续的服务不需要继续监听阻塞队列
	 */
	GROUP_CANCOMMIT_MARK(":group:cancommit_mark:"),
	
	;
	
	private String redisKey;
	
	private RedisKeyEnum(String redisKey) {
		this.redisKey = redisKey;
	}
	
	public String getRedisKey(){
		return redisKey;
	}
	
}

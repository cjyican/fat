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
	 * 是否有服务/事务出错 {@link RedisHelper }
	 */
	IS_SERVICE_ERROR(":service_error:"),
	
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
	
	/**
	 * 主线程（也就是这个接口是否已经得到各个服务返回的结果了）已经跑完？
	 * 完成了，各事务才能提交，以前那种事务等待时间，各服务节点等待返回结果的超时机制将会抛弃，由服务框架本身的超时时间与业务方法设置的事务与数据的事务时长控制
	 * fat不再干预上述流程
	 */
	MAIN_THREAD_MARK(":mainthread_mark:")
	
	;
	
	private String redisKey;
	
	private RedisKeyEnum(String redisKey) {
		this.redisKey = redisKey;
	}
	
	public String getRedisKey(){
		return redisKey;
	}
	
}

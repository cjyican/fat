package com.cjy.common.redis.constant;

import com.cjy.common.redis.TxRedisHelper;

public enum TxRedisKeyEnum {
	
	/**
	 * tx前缀
	 */
	TX_PRE("tx:"),
	
	/**
	 * 事务标识 ， 本地UUID + redis自增序列
	 */
	TX_KEY_ID("tx_id"),
	
	/**
	 * 服务标识,SET
	 */
	TX_SERVICE_ID_SET(":service_id:"),
	
	/**
	 * 是否有服务/事务出错 {@link TxRedisHelper.NORMAL}
	 */
	TX_IS_SERVICE_ERROR(":is_service_error:"),
	
	/**
	 * 加入的服务/事务列表 ， left push
	 */
	TX_SERVICE_SET(":service_set:"),
	
	/**
	 * 服务/事务的完成时间，用于反向提交事务 ， List
	 */
	TX_SERVICE_READYCOMMIT_ZSET(":readycommit_zset:"),
	
	/**
	 * 服务/事务准备提交标识，阻塞队列
	 */
	TX_SERVICE_READYCOMMIT_LIST(":readycommit_list:"),
	
	/**
	 * 服务已经到达可提交的时间线标识
	 */
	TX_SERVICE_CANCOMMIT_ZSET(":cancommit_zset:"),
	
	/**
	 * 服务/事务可提交标识，阻塞队列
	 */
	TX_SERVICE_CANCOMMIT_LIST(":cancommit_list:"),
	
	/**
	 * 是否预备（所有服务已经完成业务处理）标识，当第一个服务成功成阻塞队列中获取到元素的时候，该标识赋值为0，后续的服务不需要继续监听阻塞队列
	 */
	TX_SERVICE_READYCOMMIT_MARK(":readycommit_mark:"),
	
	/**
	 * 是否可提交（所有服已经统一时间线）标识，当第一个服务成功成阻塞队列中获取到元素的时候，该标识赋值为0，后续的服务不需要继续监听阻塞队列
	 */
	TX_SERVICE_CANCOMMIT_MARK(":cancommit_mark:"),
	
	/**
	 * 事务分组协调器管理的txKey列表 , set
	 */
	TX_GROUP_KEY_SET(":group:key_set:"),
	
	/**
	 * 事务分组协调器完成的列表，set
	 */
	TX_GROUP_FINISH_ZSET(":group:finish_zset:"),
	
	/**
	 * 是服务分组协调器完成标识 ， 阻塞队列
	 */
	TX_GROUP_CANCOMMIT_LIST(":group:cancommit_list:"),
	
	/**
	 * 该事务分组协调器管理的用于阻塞所有事务的元素
	 */
	TX_GROUP_SERVICE_SET(":group:service_set:"),
	
	/**
	 * 是否预备（所有服务已经完成业务处理）标识，当第一个服务成功成阻塞队列中获取到元素的时候，该标识赋值为0，后续的服务不需要继续监听阻塞队列
	 */
	TX_GROUP_CANCOMMIT_MARK(":group:cancommit_mark:"),
	
	;
	
	private String redisKey;
	
	private TxRedisKeyEnum(String redisKey) {
		this.redisKey = redisKey;
	}
	
	public String getRedisKey(){
		return redisKey;
	}
	
}

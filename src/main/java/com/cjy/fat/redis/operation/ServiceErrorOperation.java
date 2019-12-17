package com.cjy.fat.redis.operation;

public interface ServiceErrorOperation {
	
	/**
	 * 初始化事务回滚标识
	 * @param txKey
	 */
	public void serviceNomal();
	
	/**
	 * 事务出错
	 * @param txKey
	 */
	public void serviceError();
	
	/**
	 * 是否事务出错
	 * @param txKey
	 * @return
	 */
	public void isServiceError();

}

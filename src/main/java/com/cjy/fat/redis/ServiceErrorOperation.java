package com.cjy.fat.redis;

public interface ServiceErrorOperation {
	
	/**
	 * 初始化事务回滚标识
	 * @param txKey
	 */
	public void serviceNomal(String txKey);
	
	/**
	 * 事务出错
	 * @param txKey
	 */
	public void serviceError(String txKey);
	
	/**
	 * 是否事务出错
	 * @param txKey
	 * @return
	 */
	public void isServiceError(String txKey);

}

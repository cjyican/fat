package com.cjy.fat.redis;

public interface ServiceErrorOperation {
	
	/**
	 * 初始化事务回滚标识
	 * @param txKey
	 */
	public void txServiceNomal(String txKey);
	
	/**
	 * 事务出错
	 * @param txKey
	 */
	public void txServiceError(String txKey);
	
	/**
	 * 是否事务出错
	 * @param txKey
	 * @return
	 */
	public void isTxServiceError(String txKey);

}

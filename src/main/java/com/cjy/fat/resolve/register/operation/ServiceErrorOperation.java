package com.cjy.fat.resolve.register.operation;

public interface ServiceErrorOperation {
	
	/**
	 * 初始化事务回滚标识
	 * @param txKey
	 * @throws Exception 
	 */
	public void serviceNomal() throws Exception;
	
	/**
	 * 事务出错
	 * @param txKey
	 * @throws Exception 
	 */
	public void serviceError() throws Exception;
	
	/**
	 * 是否事务出错
	 * @param txKey
	 * @return
	 * @throws Exception 
	 */
	public void isServiceError() throws Exception;

}

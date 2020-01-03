package com.cjy.fat.resolve.register.operation;

public interface ServiceErrorOperation {
	
	/**
	 * 事务出错
	 * @param txKey
	 * @throws Exception 
	 */
	public void serviceError(String serviceName) throws Exception;
	
	/**
	 * 是否事务出错
	 * @param txKey
	 * @return
	 * @throws Exception 
	 */
	public void isServiceError() throws Exception;

}

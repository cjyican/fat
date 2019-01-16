package com.cjy.fat.redis;

public interface ServiceFinishTimeZsetOperation {

	/**
	 * 添加到服务完成列表中
	 * @param txKey
	 * @param serviceName
	 */
	public void addServiceFinishZSet(String txKey , String serviceName);
	
	/**
	 * 获取服务完成的数量
	 * @param txKey
	 * @return
	 */
	public long sizeServiceFinishZSet(String txKey);
	
	/**
	 * 
	 * @param txKey
	 * @return
	 */
	boolean isServiceFinishZSetFull(String txKey);
	
}

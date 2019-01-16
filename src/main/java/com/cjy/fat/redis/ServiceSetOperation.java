package com.cjy.fat.redis;

public interface ServiceSetOperation {
	
	/**
	 *  加入服务serviceSet
	 * @param txKey
	 * @param serviceName
	 */
	public void addToServiceSet(String txKey , String serviceName);
	
	/**
	 *  获取serviceSet的数量
	 * @return
	 */
	public long sizeServiceSet(String txKey);
	
	public void addToServiceSet(String txKey , String[] serviceNameArray);
	
}

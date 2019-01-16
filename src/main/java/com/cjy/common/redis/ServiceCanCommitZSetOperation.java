package com.cjy.common.redis;

public interface ServiceCanCommitZSetOperation {
	
	/**
	 * 加入cancommitZSet
	 * @param txKey
	 * @param content
	 */
	public void addToCancommitZSet(String txKey ,String content);
	
	/**
	 * 获取cancommitZSet的大小
	 * @param txKey
	 * @return
	 */
	public long sizeCancommitZSet(String txKey);
	
	public boolean isCancommitZSetFull(String txKey);
	
}

package com.cjy.fat.redis.operation;

public interface CanCommitListOperation {
	
	/**
	 * 获取cancommitList的大小
	 * @return
	 */
	public long sizeCancommitList(String txKey);
	
	/**
	 * 将cancommitZset的元素加入到cancommitList阻塞队列中
	 * @param txKey
	 */
	public void pushToCancommitListFromZSet(String txKey , long size);
	
}

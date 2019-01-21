package com.cjy.fat.redis.operation;

public interface ServiceCancommitListOperation {
	
	/**
	 * 将serviceSet的元素放入到CancommitList
	 */
	public void pushServiceSetToCancommitList(String txKey);
	
}

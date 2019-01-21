package com.cjy.fat.redis.operation;

public interface ServiceReadyCommitListOperation {
	
	/**
	 * 将serviceSet的元素放入到readCommitList
	 */
	public void pushServiceSetToReadCommitList(String txKey);
	
	/**
	 * 写入预备提交的阻塞队列
	 * @param txKey
	 * @param serviceName
	 */
	public void pushReadyCommitList(String txKey , String serviceName);
	
	/**
	 * 争抢预备提交标识，阻塞队列。
	 * @param txKey
	 * @return
	 */
	public String popReadyCommitList(String txKey ,long waitMilliesSecond);
	
}

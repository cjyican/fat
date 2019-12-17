package com.cjy.fat.redis.operation;

public interface GroupCanCommitListOperation {
	
	/**
	 * 将groupServiceSet的元素放入到groupCancommit
	 */
	public void pushGroupServiceSetToGroupCommitList();
	
	/**
	 * 获取事务分组协调器可提交标识
	 * @param rootTxKey
	 * @param waitMilliesSecond
	 * @return
	 */
	public String popGroupCancommit( long waitMilliesSecond) ;
	
	
}

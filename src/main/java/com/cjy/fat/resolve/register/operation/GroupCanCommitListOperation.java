package com.cjy.fat.resolve.register.operation;

public interface GroupCanCommitListOperation {
	
	/**
	 * 将groupServiceSet的元素放入到groupCancommit
	 * @throws Exception 
	 */
	public void groupCanCommit() throws Exception;
	
	/**
	 * 获取事务分组协调器可提交标识
	 * @param rootTxKey
	 * @param waitMilliesSecond
	 * @return
	 */
	public String watchGroupCanCommit( long waitMilliesSecond) throws Exception ;
	
	
}

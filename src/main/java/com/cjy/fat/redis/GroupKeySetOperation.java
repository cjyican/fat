package com.cjy.fat.redis;

public interface GroupKeySetOperation {
	/**
	 * 注册进入事务分组协调器
	 * @param rootTxKey
	 * @param localTxKey
	 */
	public void addToGroupKeySet(String rootTxKey , String localTxKey);
	
	/**
	 * 事务分组协调器维护的txKey数量
	 * @param rootTxKey
	 * @return
	 */
	public long sizeGroupKeySet(String rootTxKey) ;
}

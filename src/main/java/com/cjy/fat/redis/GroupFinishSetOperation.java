package com.cjy.fat.redis;

public interface GroupFinishSetOperation {
	
	/**
	 * 添加事务分组协调器记录完成的服务
	 * @return
	 */
	public void addGroupFinishSet(String rootTxKey , String localTxKey) ;
	
	/**
	 * 事务分组协调器完成服务的数量
	 * @param rootTxKey
	 */
	public long sizeGroupFinishSet(String rootTxKey) ;
	
	public boolean isGroupFinishZSetFull(String rootTxKey);

}

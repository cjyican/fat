package com.cjy.fat.resolve.register.operation;

public interface GroupFinishSetOperation {
	
	/**
	 * 添加事务分组协调器记录完成的服务
	 * @return
	 * @throws Exception 
	 */
	public void addToGroupFinishSet(String ele) throws Exception ;
	
	/**
	 * 事务分组协调器完成服务的数量
	 * @param rootTxKey
	 */
	public long sizeGroupFinishSet() throws Exception ;
	
	public boolean isGroupFinishZSetFull() throws Exception;

}

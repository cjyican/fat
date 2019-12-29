package com.cjy.fat.resolve.register.operation;

public interface GroupFinishSetOperation {
	
	/**
	 * 添加事务分组协调器记录完成的服务
	 * @return
	 */
	public void addToGroupFinishSet(String ele) ;
	
	/**
	 * 事务分组协调器完成服务的数量
	 * @param rootTxKey
	 */
	public long sizeGroupFinishSet() ;
	
	public boolean isGroupFinishZSetFull();

}

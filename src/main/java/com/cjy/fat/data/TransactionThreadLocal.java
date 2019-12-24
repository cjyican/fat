package com.cjy.fat.data;

public class TransactionThreadLocal {
	
	/**
	 * 根txKey
	 */
	private String rootTxKey;
	
	/**
	 * serviceId,服务标识
	 */
	private String serviceId ;
	
	/**
	 * 是否发起方
	 * @return
	 */
	private boolean leader;

	public String getRootTxKey() {
		return rootTxKey;
	}

	public void setRootTxKey(String rootTxKey) {
		this.rootTxKey = rootTxKey;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
	
	public void setLeader() {
		this.leader = true;
	}
	
	public boolean isLeader() {
		return this.leader;
	}
	
}

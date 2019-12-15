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

//	/**
//	 * 本地事务标识
//	 */
//	private Queue<String> localTxQueue = new ArrayDeque<>();

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

//	public Queue<String> getLocalTxQueue() {
//		return localTxQueue;
//	}
//
//	public void setLocalTxQueue(Queue<String> localTxQueue) {
//		this.localTxQueue = localTxQueue;
//	}
	
}

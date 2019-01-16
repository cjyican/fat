package com.cjy.common.data;

import java.util.ArrayDeque;
import java.util.Queue;

public class TxThreadLocal {
	
	/**
	 * 所属的远程分布式事务key
	 */
	private String remoteTxKey ;
	
	/**
	 * 所属的本地事务key
	 */
	private String localTxKey ;
	
	/**
	 * 根txKey
	 */
	private String rootTxKey;
	
	/**
	 * serviceId,服务标识
	 */
	private String serviceId ;

	/**
	 * 本地事务标识
	 */
	private Queue<String> localTxQueue = new ArrayDeque<>();

	public String getRemoteTxKey() {
		return remoteTxKey;
	}

	public void setRemoteTxKey(String remoteTxKey) {
		this.remoteTxKey = remoteTxKey;
	}

	public String getLocalTxKey() {
		return localTxKey;
	}

	public void setLocalTxKey(String localTxKey) {
		this.localTxKey = localTxKey;
	}

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

	public Queue<String> getLocalTxQueue() {
		return localTxQueue;
	}

	public void setLocalTxQueue(Queue<String> localTxQueue) {
		this.localTxQueue = localTxQueue;
	}
	
}

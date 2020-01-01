package com.cjy.fat.data;

/**
 * 事务协调的参数
 * 
 * @author cjy
 *
 */
public class TransactionResolveParam {

	/**
	 * 本地事务标识
	 */
	private String localTxMark;
	/**
	 * 当前txKey所属的事务分组协调器 ， 当txKey与rootTxKey相等的时候，表示不要
	 */
	private String rootTxKey;
	
	private volatile Object localRunningResult;
	
	/**
	 * 在执行业务方法时抛出的异常，在主线程中不再使用redis接收
	 */
	private volatile Exception localRunningException;

	private TransactionResolveParam() {
		
	}
	
	public static TransactionResolveParam buildTxParam(String localTxName) {
		TransactionResolveParam bean = new TransactionResolveParam();
		bean.rootTxKey = TransactionContent.getRootTxKey();
		bean.localTxMark = localTxName;
		return bean;
	}
	
	public Exception getLocalRunningException() {
		return localRunningException;
	}
	
	public void setLocalRunningException(Exception localRunningException) {
		this.localRunningException = localRunningException;
	}
	
	public String getLocalTxMark() {
		return localTxMark;
	}
	public void setLocalTxMark(String localTxMark) {
		this.localTxMark = localTxMark;
	}
	public String getRootTxKey() {
		return rootTxKey;
	}
	public void setRootTxKey(String rootTxKey) {
		this.rootTxKey = rootTxKey;
	}
	
	public void setLocalRunningResult(Object localRunningResult) {
		this.localRunningResult = localRunningResult;
	}
	
	public Object getLocalRunningResult() {
		return localRunningResult;
	}
	
}

package com.cjy.fat.data;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 事务协调的参数
 * 
 * @author cjy
 *
 */
public class TransactionResolveParam {

	/**
	 * 当前事务的txKey
	 */
	private String txKey;
	/**
	 * 本地事务标识
	 */
	private String localTxMark;
	/**
	 * 当前txKey所属的事务分组协调器 ， 当txKey与rootTxKey相等的时候，表示不要
	 */
	private String rootTxKey;
	/**
	 * 等待事务提交时长
	 */
	private long waitCommitMilliesSeconds;
	/**
	 * 等待业务操作结果时常，注意，当服务已经完成业务操作，该等待操作不允许影响整个事务组的提交与回滚
	 */
	private long waitResultMilliesSeconds;
	/**
	 * 业务方法返回值监听队列
	 * @return
	 */
	private BlockingQueue<Object> localResultQueue ;
	
	/**
	 * 在执行业务方法时抛出的异常，在主线程中不再使用redis接收
	 */
	private Exception localRunningException;

	public TransactionResolveParam(String txKey, String localTxMark, String rootTxKey,
			long waitCommitMilliesSeconds, long waitResultMilliesSeconds) {
		super();
		this.txKey = txKey;
		this.localTxMark = localTxMark;
		this.rootTxKey = rootTxKey;
		this.waitCommitMilliesSeconds = waitCommitMilliesSeconds;
		this.waitResultMilliesSeconds = waitResultMilliesSeconds;
		localResultQueue = new ArrayBlockingQueue<>(1);
	}
	
	/**
	 * 出错时，判断是否需要通知rootTxKey
	 * @return
	 */
	public boolean needToNotifyRootTxKey() {
		return this.txKey.equals(this.rootTxKey);
	}
	
	public Exception getLocalRunningException() {
		return localRunningException;
	}
	
	public void setLocalRunningException(Exception localRunningException) {
		this.localRunningException = localRunningException;
	}
	
	public long getWaitResultMilliesSeconds() {
		return waitResultMilliesSeconds;
	}
	
	public void setWaitResultMilliesSeconds(long waitResultMilliesSeconds) {
		this.waitResultMilliesSeconds = waitResultMilliesSeconds;
	}
	
	public long getWaitCommitMilliesSeconds() {
		return waitCommitMilliesSeconds;
	}
	
	public void setWaitCommitMilliesSeconds(long waitCommitMilliesSeconds) {
		this.waitCommitMilliesSeconds = waitCommitMilliesSeconds;
	}
	
	public String getTxKey() {
		return txKey;
	}
	public void setTxKey(String txKey) {
		this.txKey = txKey;
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
	
	public Object pollFromLocalResultQueue(long timeOut) throws InterruptedException {
		return localResultQueue.poll(timeOut, TimeUnit.MILLISECONDS);
	}
	
	public void offerToLocalResultQueue(Object result) {
		localResultQueue.offer(result);
	}
	
}

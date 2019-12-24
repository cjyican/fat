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
//	/**
//	 * 等待事务提交时长
//	 */
//	private long waitCommitMilliesSeconds;
//	/**
//	 * 等待业务操作结果时常，注意，当服务已经完成业务操作，该等待操作不允许影响整个事务组的提交与回滚
//	 */
//	private long waitResultMilliesSeconds;
//	/**
//	 * 业务方法返回值监听队列
//	 * @return
//	 */
//	private BlockingQueue<Object> localResultQueue = new ArrayBlockingQueue<>(1);
	
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
//		bean.waitCommitMilliesSeconds = fatTransaction.waitCommitMillisSeconds();
//		bean.waitResultMilliesSeconds = fatTransaction.waitResultMillisSeconds();
		return bean;
	}
	
	public Exception getLocalRunningException() {
		return localRunningException;
	}
	
	public void setLocalRunningException(Exception localRunningException) {
		this.localRunningException = localRunningException;
	}
	
//	public long getWaitResultMilliesSeconds() {
//		return waitResultMilliesSeconds;
//	}
//	
//	public void setWaitResultMilliesSeconds(long waitResultMilliesSeconds) {
//		this.waitResultMilliesSeconds = waitResultMilliesSeconds;
//	}
//	
//	public long getWaitCommitMilliesSeconds() {
//		return waitCommitMilliesSeconds;
//	}
//	
//	public void setWaitCommitMilliesSeconds(long waitCommitMilliesSeconds) {
//		this.waitCommitMilliesSeconds = waitCommitMilliesSeconds;
//	}
	
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
	
//	public Object pollFromLocalResultQueue(long timeOut) throws InterruptedException {
//		return localResultQueue.poll(timeOut, TimeUnit.MILLISECONDS);
//	}
//	
//	public void offerToLocalResultQueue(Object result) {
//		localResultQueue.offer(result);
//	}
	
	public void setLocalRunningResult(Object localRunningResult) {
		this.localRunningResult = localRunningResult;
	}
	
	public Object getLocalRunningResult() {
		return localRunningResult;
	}
	
}

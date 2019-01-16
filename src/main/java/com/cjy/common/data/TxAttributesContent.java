package com.cjy.common.data;

import java.util.HashMap;
import java.util.Map;

public class TxAttributesContent {
	
	/**
	 * 所属的远程分布式事务key
	 */
	public static final String STR_REMOTE_TX_KEY = "remoteTxKey";
	
	/**
	 * 根txKey
	 */
	public static final String STR_ROOT_TX_KEY = "rootTxKey";
	
	private static ThreadLocal<TxThreadLocal> txThreadLocalContainer = new ThreadLocal<>();
	
	/**
	 * 获取事务信息提供给自定义拦截器
	 * @return
	 */
	public static final Map<String , String> buildRemoteData(){
		//返回新的对象，不开放修改入口，避免被客户端串改
		Map<String , String> map = new HashMap<>();
		map.put(STR_REMOTE_TX_KEY, getLocalTxKey());
		map.put(STR_ROOT_TX_KEY, getRootTxKey());
		return map;
	}
	
	/**
	 * 设置远程分布式key
	 * @param txKey
	 * @return
	 */
	public static void setRemoteTxKey(String remoteTxKey) {
		txThreadLocalContainer.get().setRemoteTxKey(remoteTxKey);
	}
	
	/**
	 * 获取远程分布式key
	 */
	public static String getRemoteTxKey() {
		return txThreadLocalContainer.get().getRemoteTxKey();
	}
	
	/**
	 * 设置本地分布式key
	 * @param txKey
	 * @return
	 */
	public static void setLocalTxKey(String localTxKey) {
		txThreadLocalContainer.get().setLocalTxKey(localTxKey);
	}
	
	/**
	 * 获取本地分布式key
	 */
	public static String getLocalTxKey() {
		return  txThreadLocalContainer.get().getLocalTxKey();
	}
	
	public static void setRootTxKey(String rootTxKey) {
		txThreadLocalContainer.get().setRootTxKey(rootTxKey);
	}

	public static String getRootTxKey() {
		return txThreadLocalContainer.get().getRootTxKey();
	}
	
	/**
	 * 本地事务组加入元素
	 */
	public static void pushLocalTxQueue(String localTxMark){
		txThreadLocalContainer.get().getLocalTxQueue().add(localTxMark);
	}
	
	/**
	 * 本地事务组取出元素
	 */
	public static String pollLocalTxQueue(){
		return txThreadLocalContainer.get().getLocalTxQueue().poll();
	}
	
	/**
	 * 本地事务组数量
	 */
	public static int localTxQueueSize(){
		return txThreadLocalContainer.get().getLocalTxQueue().size();
	}
	
	/**
	 * 获取服务标识
	 */
	public static String getServiceId(){
		return txThreadLocalContainer.get().getServiceId();
	}
	
	/**
	 * 设置服务标识
	 */
	public static void setServiceId(String serviceId){
		txThreadLocalContainer.get().setServiceId(serviceId);
	}
	
	/**
	 * 由于使用了线程池，当线程复用的时候，TheadLocal依然存在，需要在请求入口清空ThreadLocal
	 */
	public static final void initContainer(){
		txThreadLocalContainer.remove();
		txThreadLocalContainer.set(new TxThreadLocal());
	}
	
}

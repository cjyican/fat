package com.cjy.common.redis;

public interface NeedFinishCountOperation {
	
	/**
	 * 增加需要完成的服务数量
	 * @param txKey
	 */
	public void incrNeedServiceFinishNum(String txKey);
	
}

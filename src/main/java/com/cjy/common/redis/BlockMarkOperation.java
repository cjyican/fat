package com.cjy.common.redis;

import com.cjy.common.redis.constant.TxRedisKeyEnum;

public interface BlockMarkOperation {
	
	void passBlockMark(String txKey ,TxRedisKeyEnum markEnum);
	
	void unPassBlockMark(String txKey , TxRedisKeyEnum markEnum);
	
	boolean isBlockMarkPassed(String txKey ,TxRedisKeyEnum markEnum);
	
}

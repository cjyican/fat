package com.cjy.fat.redis;

import com.cjy.fat.redis.constant.TxRedisKeyEnum;

public interface BlockMarkOperation {
	
	void passBlockMark(String txKey ,TxRedisKeyEnum markEnum);
	
	void unPassBlockMark(String txKey , TxRedisKeyEnum markEnum);
	
	boolean isBlockMarkPassed(String txKey ,TxRedisKeyEnum markEnum);
	
}

package com.cjy.fat.redis;

import com.cjy.fat.redis.constant.RedisKeyEnum;

public interface BlockMarkOperation {
	
	void passBlockMark(String txKey ,RedisKeyEnum markEnum);
	
	void unPassBlockMark(String txKey , RedisKeyEnum markEnum);
	
	boolean isBlockMarkPassed(String txKey ,RedisKeyEnum markEnum);
	
}

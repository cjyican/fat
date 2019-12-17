package com.cjy.fat.redis.operation;

public interface BlockMarkOperation {
	
	void passBlockMark();
	
	void unPassBlockMark();
	
	boolean isBlockMarkPassed();
	
}

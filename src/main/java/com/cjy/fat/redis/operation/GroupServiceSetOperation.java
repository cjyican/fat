package com.cjy.fat.redis.operation;

public interface GroupServiceSetOperation {
	
	void addToGroupServiceSet(String ele);

	long sizeGroupSeviceSet();
	
}

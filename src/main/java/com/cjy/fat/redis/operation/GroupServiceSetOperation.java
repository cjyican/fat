package com.cjy.fat.redis.operation;

public interface GroupServiceSetOperation {
	
	void addLocalServiceSetToGroupServiceSet(String localTxKey , String rootKey);
	
	void addToGroupServiceSet(String rootTxKey , String serviceId);
	
}

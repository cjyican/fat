package com.cjy.fat.redis.operation;

public interface GroupServiceSetOperation {
	
	void addToGroupServiceSet(String rootTxKey , String serviceId);
	
	void addToGroupServiceSet(String rootTxKey , String ... serviceIds);


	long sizeGroupSeviceSet();
	
}

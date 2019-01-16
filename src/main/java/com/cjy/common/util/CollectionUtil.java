package com.cjy.common.util;

import java.util.Set;

import com.cjy.common.exception.TxException;

public class CollectionUtil {
	
	public static void checkDataSet(Set<String> dataSet , String txKey){
		if(null == dataSet || dataSet.size() == 0 ){
			throw new TxException(txKey , "empty service list");
		}
	}
	
}

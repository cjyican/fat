package com.cjy.fat.util;

import java.util.Set;

import com.cjy.fat.exception.FatTransactionException;

public class CollectionUtil {
	
	private CollectionUtil(){
		
	}
	
	public static void checkDataSet(Set<String> dataSet , String txKey){
		if(null == dataSet || dataSet.size() == 0 ){
			throw new FatTransactionException(txKey , "empty service list");
		}
	}
	
}

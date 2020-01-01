package com.cjy.fat.util;

import org.apache.commons.lang3.StringUtils;

public class StringUtil {
	
	static int txKeyLegth = 10;
	
	private StringUtil(){
		
	}
	
	public static String appendStr(String ...strings ){
		String s = "";
		for(int i = 0 ; i < strings.length ;i ++){
			s += strings[i];
		}
		return s;
	}
	
	public static String initTxKey(String seq) {
		if(StringUtils.isBlank(seq)) {
			return seq;
		}
		
		int lessLength = txKeyLegth - seq.length();
		
		if(lessLength <= 0) {
			return seq;
		}
		
		String txKeyPre = "";
		
		if(lessLength > 0) {
			
			while(lessLength > 0) {
				txKeyPre = txKeyPre +  "0";
				
				lessLength --;
			}
			
			txKeyPre = txKeyPre + seq; 
			
		}
		
		return txKeyPre;
	}
	
	
}

package com.cjy.fat.resolve.register.servicenode;

import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.util.StringUtil;

public class RedisNameSpaceAppender implements NameSpaceAppender{
	
	public static final String REDIS_PRE = ":";

	@Override
	public String appendNameSpace(NameSpace nameSpace) {
		return StringUtil.appendStr(
				NameSpace.FAT_PRE.getNameSpace() ,
				REDIS_PRE , 
				TransactionContent.getRootTxKey() ,
				REDIS_PRE ,
				nameSpace.getNameSpace() );
	}

}

package com.cjy.fat.resolve.register.servicenode;

import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.util.StringUtil;

public class ZookeeperNameSpaceAppender implements NameSpaceAppender{

	public static final String ZOOKEEPER_PRE = "/";
	
	@Override
	public String appendNameSpace(NameSpace nameSpace) {
		return StringUtil.appendStr(
				ZOOKEEPER_PRE ,
				NameSpace.FAT_PRE.getNameSpace() ,
				ZOOKEEPER_PRE , 
				TransactionContent.getRootTxKey() ,
				ZOOKEEPER_PRE
				,nameSpace.getNameSpace() 
				);
	}

}

package com.cjy.fat.resolve.register;

import com.cjy.fat.resolve.register.operation.GroupCanCommitListOperation;
import com.cjy.fat.resolve.register.operation.GroupFinishSetOperation;
import com.cjy.fat.resolve.register.operation.GroupServiceSetOperation;
import com.cjy.fat.resolve.register.operation.ServiceErrorOperation;
import com.cjy.fat.resolve.register.servicenode.NameSpace;
import com.cjy.fat.resolve.register.servicenode.NameSpaceAppender;

public abstract class AbstractRegister implements ServiceRegister , NameSpaceAppender{

	private NameSpaceAppender nameSpaceAppender;
	
	AbstractRegister(){
		
	}
	
	AbstractRegister(NameSpaceAppender nameSpaceAppender ){
		this.nameSpaceAppender = nameSpaceAppender;
	}
	
	protected GroupCanCommitListOperation groupCanCommitListOperation;
	
	protected ServiceErrorOperation serviceErrorOperation;
	
	protected GroupFinishSetOperation groupFinishSetOperation;
	
	protected GroupServiceSetOperation groupServiceSetOperation ; 
	
	@Override
	public String appendNameSpace(NameSpace nameSpace) {
		return nameSpaceAppender.appendNameSpace(nameSpace);
	};
	
}

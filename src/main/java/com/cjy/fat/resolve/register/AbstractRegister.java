package com.cjy.fat.resolve.register;

import com.cjy.fat.resolve.register.operation.GroupCanCommitListOperation;
import com.cjy.fat.resolve.register.operation.GroupFinishSetOperation;
import com.cjy.fat.resolve.register.operation.GroupServiceSetOperation;
import com.cjy.fat.resolve.register.operation.ServiceErrorOperation;
import com.cjy.fat.resolve.register.servicenode.NameSpace;

public abstract class AbstractRegister implements ServiceRegister{
	
	AbstractRegister(){
		
	}
	
	public String rootNameSpace ;
	
	public String getRootNameSpace() {
		return rootNameSpace;
	}
	
	protected abstract void setRootNameSpace ();
	
	protected abstract void initRootNameSpace () throws Exception ;
	
	protected abstract String appendNameSpace(NameSpace nameSpace) ;
	
	protected GroupCanCommitListOperation groupCanCommitListOperation;
	
	protected ServiceErrorOperation serviceErrorOperation;
	
	protected GroupFinishSetOperation groupFinishSetOperation;
	
	protected GroupServiceSetOperation groupServiceSetOperation ; 
	
	
	
}

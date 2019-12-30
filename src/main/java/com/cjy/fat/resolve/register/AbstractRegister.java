package com.cjy.fat.resolve.register;

import com.cjy.fat.resolve.register.operation.GroupCanCommitListOperation;
import com.cjy.fat.resolve.register.operation.GroupFinishSetOperation;
import com.cjy.fat.resolve.register.operation.GroupServiceSetOperation;
import com.cjy.fat.resolve.register.operation.MainThreadMarkOperation;
import com.cjy.fat.resolve.register.operation.ServiceErrorOperation;

public abstract class AbstractRegister implements ServiceRegister{

	protected GroupCanCommitListOperation groupCanCommitListOperation;
	
	protected ServiceErrorOperation serviceErrorOperation;
	
	protected GroupFinishSetOperation groupFinishSetOperation;
	
	protected GroupServiceSetOperation groupServiceSetOperation ; 
	
	protected MainThreadMarkOperation mainThreadMarkOperation;
	
}

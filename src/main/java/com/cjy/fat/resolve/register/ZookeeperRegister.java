package com.cjy.fat.resolve.register;

import org.springframework.stereotype.Component;

import com.cjy.fat.config.ZooTemplate;
import com.cjy.fat.resolve.register.operation.GroupCanCommitListOperation;
import com.cjy.fat.resolve.register.operation.GroupFinishSetOperation;
import com.cjy.fat.resolve.register.operation.GroupServiceSetOperation;
import com.cjy.fat.resolve.register.operation.MainThreadMarkOperation;
import com.cjy.fat.resolve.register.operation.ServiceErrorOperation;
import com.cjy.fat.resolve.register.servicenode.ServiceNameSpace;

@Component
public class ZookeeperRegister extends AbstractRegister{
	
	@Override
	public String createTxKey(ServiceNameSpace nameSpace) throws Exception{
		return ZooTemplate.createSeqNode(nameSpace);
	}

	@Override
	public ServiceErrorOperation opsForServiceError() {
		if(null == serviceErrorOperation) {
			serviceErrorOperation = new ServiceErrorOperation() {
				
				@Override
				public void serviceNomal() {
					ZooTemplate.getNodeData();
				}
				
				@Override
				public void serviceError() {
					
				}
				
				@Override
				public void isServiceError() {
					
				}
			};
		}
		return serviceErrorOperation;
	}

	@Override
	public GroupCanCommitListOperation opsForGroupCanCommitListOperation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GroupFinishSetOperation opsForGroupFinishSetOperation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GroupServiceSetOperation opsForGroupServiceSetOperation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MainThreadMarkOperation opsForMainThreadMarkOperation() {
		// TODO Auto-generated method stub
		return null;
	}

}

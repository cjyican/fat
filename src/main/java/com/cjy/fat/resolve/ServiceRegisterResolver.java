package com.cjy.fat.resolve;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cjy.fat.annotation.FatServiceRegister;
import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.resolve.register.ServiceRegister;

@Component
public class ServiceRegisterResolver {

	@Autowired
	ServiceRegister register;
	
	public void registerService(FatServiceRegister txRegisterService ) throws Exception{
		if(!txRegisterService.openTransaction()) {
			return;
		}
		
		if (StringUtils.isEmpty(TransactionContent.getRootTxKey())) {
			String rootTxKey = register.createTxKey();
			TransactionContent.setRootTxKey(rootTxKey);
			
			// 初始化事务组回滚标识
//			register.opsForServiceError().serviceNomal();
			
		}
		
		// 将自己加入事务组
		register.opsForGroupServiceSetOperation().addToGroupServiceSet(TransactionContent.getServiceId());
		
	}
	
	
}

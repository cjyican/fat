package com.cjy.fat.resolve;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cjy.fat.annotation.FatServiceRegister;
import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.resolve.register.RedisRegister;

@Component
public class ServiceRegisterResolver {

	@Autowired
	RedisRegister redisRegister;
	
	public void registerService(FatServiceRegister txRegisterService ){
		if(!txRegisterService.openTransaction()) {
			return;
		}
		
		if (StringUtils.isEmpty(TransactionContent.getRootTxKey())) {
			String rootTxKey = redisRegister.createTxKey(TransactionContent.getServiceId());
			TransactionContent.setRootTxKey(rootTxKey);
			
			// 初始化事务组回滚标识
			redisRegister.opsForServiceError().serviceNomal();
			
			// 设置leader
			TransactionContent.setLeader();
		}
		
		// 将自己加入事务组
		redisRegister.opsForGroupServiceSetOperation().addToGroupServiceSet(TransactionContent.getServiceId());
		
	}
	
	
}

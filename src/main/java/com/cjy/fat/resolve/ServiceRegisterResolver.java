package com.cjy.fat.resolve;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cjy.fat.annotation.FatServiceRegister;
import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.redis.RedisHelper;

@Component
public class ServiceRegisterResolver {

	@Autowired
	RedisHelper redisHelper;
	
	public void registerService(FatServiceRegister txRegisterService ){
		if(!txRegisterService.openTransaction()) {
			return;
		}
		
		if (StringUtils.isEmpty(TransactionContent.getRootTxKey())) {
			String rootTxKey = redisHelper.createTxKey(TransactionContent.getServiceId());
			TransactionContent.setRootTxKey(rootTxKey);
			
			// 初始化事务组回滚标识
			redisHelper.opsForServiceError().serviceNomal();
			
			// 设置leader
			TransactionContent.setLeader();
		}
		
		// 将自己加入事务组
		redisHelper.opsForGroupServiceSetOperation().addToGroupServiceSet(TransactionContent.getServiceId());
		
	}
	
	
}

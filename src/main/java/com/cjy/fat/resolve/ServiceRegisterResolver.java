package com.cjy.fat.resolve;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cjy.fat.annotation.FatServiceRegister;
import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.redis.RedisHelper;
import com.cjy.fat.util.StringUtil;

@Component
public class ServiceRegisterResolver {
	
	private static final String localTxMarkFont = "-localTX-";

	@Autowired
	RedisHelper redisHelper;
	
	private static final String SERVICE = "-service-";
	
	public void registerService(FatServiceRegister txRegisterService ){
		//该服务设置的远程服务数量
		int serviceCount = txRegisterService.serviceCount();
		int localTransactionCount = txRegisterService.localTransactionCount();
		if(serviceCount == 0 && localTransactionCount == 0) {
			//不调用服务，没本地事务，加什么@TxRegisterService注解啊,直接给他return
			return;
		}
		if (serviceCount > 0) {
			//生成该次分布式事务组的标识txKey，由于这里包含调用其他服务，所以命名为localTxKey
			String localTxKey = redisHelper.createTxKey(TransactionContent.getServiceId());
			
			// 加入线程变量，后面的ServiceRunningHandler都将使用该localTxKey
			TransactionContent.setLocalTxKey(localTxKey);
			
			// 当只有localTxKey,没有remoteTxKey的时候，表示这个是最初的服务发起方 ，是rootTxKey
			if(StringUtils.isBlank(TransactionContent.getRemoteTxKey())) {
				TransactionContent.setRootTxKey(localTxKey);
			}
			
			String[] serviceIds = getServiceIdArray(serviceCount, localTxKey);
			// 添加分布式服务标识
			redisHelper.addToTxServiceIdSet(localTxKey , serviceIds);
			// 添加进入事务组
			redisHelper.opsForGroupServiceSetOperation().addToGroupServiceSet(TransactionContent.getRootTxKey(), serviceIds);
			if(TransactionContent.getLocalTxKey().equals(TransactionContent.getRootTxKey())) {
				// 初始化事务组回滚标识
				redisHelper.opsForServiceError().serviceNomal(localTxKey);
				// 将自己加入事务组
				redisHelper.opsForGroupServiceSetOperation().addToGroupServiceSet(TransactionContent.getRootTxKey(), TransactionContent.getLocalTxKey());
			}
		}
		// 生成本地事务标识
		for(int i = 0 ; i < localTransactionCount ;i ++) {
			String localTxMark = StringUtil.appendStr(TransactionContent.getServiceId() , localTxMarkFont , i+"");
			TransactionContent.pushLocalTxQueue(localTxMark);
			// 添加进入事务组
			redisHelper.opsForGroupServiceSetOperation().addToGroupServiceSet(TransactionContent.getRootTxKey(), localTxMark);
		}
	}
	
	private String[] getServiceIdArray(int serviceCount , String txKey) {
		String[] array = new String[serviceCount];
		for(int i = 0 ; i < serviceCount ; i ++){
			array[i] = txKey + SERVICE + i;
		}
		return array;
	}
	
}

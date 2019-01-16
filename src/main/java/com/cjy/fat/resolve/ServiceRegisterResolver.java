package com.cjy.fat.resolve;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cjy.fat.annotation.FatServiceRegister;
import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.redis.TxRedisHelper;
import com.cjy.fat.util.StringUtil;

@Component
public class ServiceRegisterResolver {
	
	private static final String localTxMarkFont = "-localTX-";

	@Autowired
	TxRedisHelper txRedisHelper;
	
	public void registerService(FatServiceRegister txRegisterService ){
		//该服务设置的远程服务数量
		int serviceCount = txRegisterService.serviceCount();
		int localTxCount = txRegisterService.localTxCount();
		if(serviceCount == 0 && localTxCount == 0) {
			//不调用服务，没本地事务，加什么@TxRegisterService注解啊,直接给他return
			return;
		}
		if (serviceCount > 0) {
			//生成该次分布式事务组的标识txKey，由于这里包含调用其他服务，所以命名为localTxKey
			String localTxKey = txRedisHelper.createTxKey(TransactionContent.getServiceId());
			// 初始化事务组回滚标识
			txRedisHelper.opsForServiceError().txServiceNomal(localTxKey);
			// 加入线程变量，后面的ServiceRunningHandler都将使用该localTxKey
			TransactionContent.setLocalTxKey(localTxKey);
			txRedisHelper.addToTxServiceSet(localTxKey , serviceCount);
			// 将自己加入服务列表
			txRedisHelper.opsForServiceSetOperation().addToServiceSet(localTxKey, TransactionContent.getServiceId());
			
			// 当只有localTxKey,没有remoteTxKey的时候，表示这个是最初的服务发起方 ，是rootTxKey
			if(StringUtils.isBlank(TransactionContent.getRemoteTxKey())) {
				TransactionContent.setRootTxKey(localTxKey);
			}
			// 本地localTxKey(一个localTxKey表示一个事务组，存在多个localTxKey表示存在父子嵌套事务组) ，注册进入事务分组协调器
			txRedisHelper.opsForGroupKeySetOperation().addToGroupKeySet(TransactionContent.getRootTxKey(), localTxKey);
			// 加入到Group分组协调器的serviceSet中
			txRedisHelper.opsForGroupServiceSetOperation().addLocalServiceSetToGroupServiceSet(TransactionContent.getLocalTxKey(), TransactionContent.getRootTxKey());
		}
		// 生成本地事务标识
		for(int i = 0 ; i < localTxCount ;i ++) {
			String localTxMark = StringUtil.appendStr(TransactionContent.getServiceId() , localTxMarkFont , i+"");
			TransactionContent.pushLocalTxQueue(localTxMark);
			if(StringUtils.isNotBlank(TransactionContent.getLocalTxKey())) {//当产生了localTxKey，需要继续调用服务的时候，需要把mark加入redis作为cancomlist的元素serviceSet
				// 加入服务列表 ，增加需要完成的服务数量
				txRedisHelper.opsForServiceSetOperation().addToServiceSet(TransactionContent.getLocalTxKey(), localTxMark);
			}
			//存在远程txKey，被调用
			if(StringUtils.isNotBlank(TransactionContent.getRemoteTxKey())) {
				//当不产生新的事务组时，加入到上层事务组服务列表，产生了新的事务组，则不加入，因为这个被自己的事务组管理了
				if(StringUtils.isBlank(TransactionContent.getLocalTxKey())) {
					txRedisHelper.opsForServiceSetOperation().addToServiceSet(TransactionContent.getRemoteTxKey(), localTxMark);
				}
			}
			txRedisHelper.opsForGroupServiceSetOperation().addToGroupServiceSet(TransactionContent.getRootTxKey(), localTxMark);
		}
	}
}

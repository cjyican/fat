package com.cjy.fat.resolve.accept;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.rpc.RpcContext;
import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.redis.RedisHelper;

@Component
@ConditionalOnClass({RpcContext.class})
public class DubboRemoteTransactionAccepter implements RemoteTransactionAccepter {
	
	@Autowired
	RedisHelper redisHelper;
	
	@Value("${spring.application.name}")
	String serviceName;

	@Override
	public void acceptRemoteTransactionData() {
		RpcContext context = RpcContext.getContext() ;
		
		if(null == context) {
			return ; 
		}
		
        Map<String, String> attachments = context.getAttachments();
        //获取remoteTxKey
  		String remoteTxKey = attachments.get(TransactionContent.STR_REMOTE_TX_KEY);
  		if(StringUtils.isNotBlank(remoteTxKey)){
  			//加入本地线程remoteTxkey变量
  			TransactionContent.setRemoteTxKey(remoteTxKey);
  		}
  		// 获取rootTxKey
  		String rootTxKey = attachments.get(TransactionContent.STR_ROOT_TX_KEY);
  		if(StringUtils.isNotBlank(rootTxKey)){
  			TransactionContent.setRootTxKey(rootTxKey);
  		}
  		// 获取serviceId
  		String serviceId = redisHelper.popFromServiceIdSet(remoteTxKey);
  		if(StringUtils.isBlank(serviceId)){
  			serviceId = serviceName;
  		}
  		TransactionContent.setServiceId(serviceId);
	}

}

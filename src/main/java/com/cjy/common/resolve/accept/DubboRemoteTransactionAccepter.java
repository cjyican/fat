package com.cjy.common.resolve.accept;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.rpc.RpcContext;
import com.cjy.common.data.TxAttributesContent;
import com.cjy.common.redis.TxRedisHelper;

@Component
@ConditionalOnClass({RpcContext.class})
public class DubboRemoteTransactionAccepter implements RemoteTransactionAccepter {
	
	@Autowired
	TxRedisHelper txRedisHelper;
	
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
  		String remoteTxKey = attachments.get(TxAttributesContent.STR_REMOTE_TX_KEY);
  		if(StringUtils.isNotBlank(remoteTxKey)){
  			//加入本地线程remoteTxkey变量
  			TxAttributesContent.setRemoteTxKey(remoteTxKey);
  		}
  		// 获取rootTxKey
  		String rootTxKey = attachments.get(TxAttributesContent.STR_ROOT_TX_KEY);
  		if(StringUtils.isNotBlank(rootTxKey)){
  			TxAttributesContent.setRootTxKey(rootTxKey);
  		}
  		// 获取serviceId
  		String serviceId = txRedisHelper.popFromServiceIdSet(remoteTxKey);
  		if(StringUtils.isBlank(serviceId)){
  			serviceId = serviceName;
  		}
  		TxAttributesContent.setServiceId(serviceId);
	}

}

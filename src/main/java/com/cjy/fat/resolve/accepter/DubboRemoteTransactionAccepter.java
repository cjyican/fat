package com.cjy.fat.resolve.accepter;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.rpc.RpcContext;
import com.cjy.fat.data.TransactionContent;

@Component
@ConditionalOnClass({RpcContext.class})
public class DubboRemoteTransactionAccepter implements RemoteTransactionAccepter {
	
	@Value("${spring.application.name}")
	String serviceName;

	@Override
	public void acceptRemoteTransactionData() {
		RpcContext context = RpcContext.getContext() ;
		
		if(null == context) {
			return ; 
		}
		
        Map<String, String> attachments = context.getAttachments();
        
  		// 获取rootTxKey
  		String rootTxKey = attachments.get(TransactionContent.STR_ROOT_TX_KEY);
  		if(StringUtils.isNotBlank(rootTxKey)){
  			TransactionContent.setRootTxKey(rootTxKey);
  		}
  		
  		TransactionContent.setServiceId(serviceName);
	}

}

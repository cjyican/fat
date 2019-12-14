package com.cjy.fat.resolve.accept;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.redis.RedisHelper;

@Component
@ConditionalOnClass({EnableDiscoveryClient.class})
public class SpringCloudRemoteTransactionAccepter implements RemoteTransactionAccepter{
	
	@Autowired
	RedisHelper redisHelper;
	
	@Value("${spring.application.name}")
	String serviceName;

	@Override
	public void acceptRemoteTransactionData() {

	    ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
	    if(sra == null) {
	    	return;
	    }
	    HttpServletRequest request = sra.getRequest();
	    
		// 获取根rootTxKey,可能会存在一个接口，调用多条服务链路的情况
		String rootTxKey = request.getHeader(TransactionContent.STR_ROOT_TX_KEY);
		if(StringUtils.isNotBlank(rootTxKey)){
			TransactionContent.setRootTxKey(rootTxKey);
		}
		// 获取serviceId（客户端生成的调用服务的标识，避免重复调用服务，导致的服务标识重复问题）
		String serviceId = redisHelper.popFromServiceIdSet(rootTxKey);
		if(StringUtils.isBlank(serviceId)){
			serviceId = serviceName;
		}
		TransactionContent.setServiceId(serviceId);
	}
	
	
	
	
}

package com.cjy.fat.interceptor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.resolve.RegisterAspect;

import feign.Feign;
import feign.RequestInterceptor;
import feign.RequestTemplate;

@Component
@ConditionalOnClass({RegisterAspect.class, Feign.class }) // 当使用TxAspect增强客户端的分布式事务时加载
public class FatSpringCloudInterceptor implements RequestInterceptor {

	/**
	 * 放入Feign的Request , 本地事务localTxKey作为服务方的RemoteTxKey
	 */
	@Override
	public void apply(RequestTemplate template) {
		//设置rootTxKey传递给下方的服务
		if (StringUtils.isNotBlank(TransactionContent.getRootTxKey())) {
			template.header(TransactionContent.STR_ROOT_TX_KEY, TransactionContent.getRootTxKey());
		}
	}

}

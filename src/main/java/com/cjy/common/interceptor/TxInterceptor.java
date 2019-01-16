package com.cjy.common.interceptor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import com.cjy.common.data.TxAttributesContent;
import com.cjy.common.resolve.TxRegisterAspect;

import feign.Feign;
import feign.RequestInterceptor;
import feign.RequestTemplate;

@Component
@ConditionalOnClass({TxRegisterAspect.class, Feign.class }) // 当使用TxAspect增强客户端的分布式事务时加载
public class TxInterceptor implements RequestInterceptor {

	/**
	 * 放入Feign的Request , 本地事务localTxKey作为服务方的RemoteTxKey
	 */
	@Override
	public void apply(RequestTemplate template) {
		if (StringUtils.isNotBlank(TxAttributesContent.getLocalTxKey())) {
			template.header(TxAttributesContent.STR_REMOTE_TX_KEY, TxAttributesContent.getLocalTxKey());
		}
		//设置rootTxKey传递给下方的服务
		if (StringUtils.isNotBlank(TxAttributesContent.getRootTxKey())) {
			template.header(TxAttributesContent.STR_ROOT_TX_KEY, TxAttributesContent.getRootTxKey());
		}
	}

}

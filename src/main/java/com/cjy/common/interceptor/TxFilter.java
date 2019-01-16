package com.cjy.common.interceptor;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.cjy.common.data.TxAttributesContent;

@Activate(group = {Constants.CONSUMER } , order = -100001 )
public class TxFilter implements Filter{
	
	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		if (StringUtils.isNotBlank(TxAttributesContent.getLocalTxKey())) {
			RpcContext.getContext().setAttachment(TxAttributesContent.STR_REMOTE_TX_KEY, TxAttributesContent.getLocalTxKey());
		}
		if (StringUtils.isNotBlank(TxAttributesContent.getRootTxKey())) {
			RpcContext.getContext().setAttachment(TxAttributesContent.STR_ROOT_TX_KEY, TxAttributesContent.getRootTxKey());
		}
		return invoker.invoke(invocation);
	}

}

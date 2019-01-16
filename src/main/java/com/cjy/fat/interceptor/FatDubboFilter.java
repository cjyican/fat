package com.cjy.fat.interceptor;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.cjy.fat.data.TransactionContent;

@Activate(group = {Constants.CONSUMER } , order = -100001 )
public class FatDubboFilter implements Filter{
	
	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		if (StringUtils.isNotBlank(TransactionContent.getLocalTxKey())) {
			RpcContext.getContext().setAttachment(TransactionContent.STR_REMOTE_TX_KEY, TransactionContent.getLocalTxKey());
		}
		if (StringUtils.isNotBlank(TransactionContent.getRootTxKey())) {
			RpcContext.getContext().setAttachment(TransactionContent.STR_ROOT_TX_KEY, TransactionContent.getRootTxKey());
		}
		return invoker.invoke(invocation);
	}

}

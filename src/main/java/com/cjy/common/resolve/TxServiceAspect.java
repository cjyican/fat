package com.cjy.common.resolve;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cjy.common.annotation.TxService;
import com.cjy.common.data.TxAttributesContent;
import com.cjy.common.data.TxResolveParam;
import com.cjy.common.exception.TxException;
import com.cjy.common.redis.TxRedisHelper;
import com.cjy.common.resolve.handler.ServiceRunningHandler;

@Aspect
@Component
@ConditionalOnClass(value = { DataSourceTransactionManager.class })
@Order(36)
public class TxServiceAspect {
	
	@Autowired
	TxRedisHelper txRedisHelper;

	@Autowired
	ServiceRunningHandler serviceHandler;
	
	@Autowired
	TxCommitResolver commitResolver;

	@Pointcut("@annotation(txService)")
	public void txService(TxService txService) {

	}

	@Around("txService(txService)")
	public Object doAround(JoinPoint joinPoint, TxService txService) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Class<?> returnType = signature.getReturnType();
		ProceedingJoinPoint proceedingJoinPoint = (ProceedingJoinPoint) joinPoint;
		
		//获取TxKey , 当该接口具有本地事务时使用LocalKey , 当localKey不存在，说明不需要使用本地事务 ， 使用remoteKey
		String txKey = TxAttributesContent.getLocalTxKey();
		String rootTxKey = TxAttributesContent.getRootTxKey();
		String serviceId = TxAttributesContent.getServiceId();
		String localTxMark = null ;
		//获取本地事务标识
		localTxMark =  TxAttributesContent.pollLocalTxQueue();
		//查看是否获取到本地事务标识，若获取不到，则使用applicationName 
		if(StringUtils.isBlank(localTxMark)){
			localTxMark = serviceId;
		}
		if(StringUtils.isBlank(txKey)){
			//没有localTxKey说明这个仅仅作为服务被调用，不再调用其它服务，则该次分布式事务组的标识为远程传过来的remoteTxKey
			txKey = TxAttributesContent.getRemoteTxKey();
		}
		//当txKey不存在时，没有远程穿过的，也没有本地生成的，说明客户端没有开启分布式事务 , 直接运行
		if(StringUtils.isBlank(txKey)){
			return proceedingJoinPoint.proceed();
		}
		Method serviceMethod = signature.getMethod();
		Transactional transactionalAnno = serviceMethod.getAnnotation(Transactional.class);
		if(null == transactionalAnno) {
			txRedisHelper.opsForServiceError().txServiceError(txKey);
			throw new TxException("the method " + serviceMethod.getName() + " is not decorated by @Transactional");
		}
		TxResolveParam param = new TxResolveParam(
				txKey, 
				localTxMark, 
				rootTxKey, 
				txService.waitCommitMillisSeconds(), 
				txService.waitResultMillisSeconds(),
				returnType);
		// 异步执行业务操作
		serviceHandler.proceed(proceedingJoinPoint , transactionalAnno, param ,TxAttributesContent.buildRemoteData());
		return commitResolver.waitServiceResult(param);
	}
	
	
}

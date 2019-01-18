package com.cjy.fat.resolve;

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

import com.cjy.fat.annotation.FatTransaction;
import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.data.TransactionResolveParam;
import com.cjy.fat.exception.FatTransactionException;
import com.cjy.fat.redis.RedisHelper;
import com.cjy.fat.resolve.handler.ServiceRunningHandler;

@Aspect
@Component
@ConditionalOnClass(value = { DataSourceTransactionManager.class })
@Order(36)
public class TransactionAspect {
	
	@Autowired
	RedisHelper redisHelper;

	@Autowired
	ServiceRunningHandler serviceHandler;
	
	@Autowired
	CommitResolver commitResolver;

	@Pointcut("@annotation(txService)")
	public void txService(FatTransaction txService) {

	}

	@Around("txService(txService)")
	public Object doAround(JoinPoint joinPoint, FatTransaction txService) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Class<?> returnType = signature.getReturnType();
		ProceedingJoinPoint proceedingJoinPoint = (ProceedingJoinPoint) joinPoint;
		
		//获取TxKey , 当该接口具有本地事务时使用LocalKey , 当localKey不存在，说明不需要使用本地事务 ， 使用remoteKey
		String txKey = TransactionContent.getLocalTxKey();
		String rootTxKey = TransactionContent.getRootTxKey();
		//String serviceId = TransactionContent.getServiceId();
		String localTxMark = null ;
		if(StringUtils.isBlank(txKey)){
			//没有localTxKey说明这个仅仅作为服务被调用，不再调用其它服务，则该次分布式事务组的标识为远程传过来的remoteTxKey
			txKey = TransactionContent.getRemoteTxKey();
		}
		//当txKey不存在时，没有远程穿过的，也没有本地生成的，说明客户端没有开启分布式事务 , 直接运行
		if(StringUtils.isBlank(txKey)){
			return proceedingJoinPoint.proceed();
		}
		//获取本地事务标识
		localTxMark =  TransactionContent.pollLocalTxQueue();
		//查看是否获取到本地事务标识，若获取不到，说明配置过少
		if(StringUtils.isBlank(localTxMark)){
			//localTxMark = serviceId;
			throw new FatTransactionException(txKey , "could not poll localTxMark ,the config local transaction count is less than real count" ); 
		}
		Method serviceMethod = signature.getMethod();
		Transactional transactionalAnno = serviceMethod.getAnnotation(Transactional.class);
		if(null == transactionalAnno) {
			throw new FatTransactionException("the method " + serviceMethod.getName() + " is not decorated by @Transactional");
		}
		TransactionResolveParam param = new TransactionResolveParam(
				txKey, 
				localTxMark, 
				rootTxKey, 
				txService.waitCommitMillisSeconds(), 
				txService.waitResultMillisSeconds(),
				returnType);
		// 异步执行业务操作
		serviceHandler.proceed(proceedingJoinPoint , transactionalAnno, param ,TransactionContent.buildRemoteData());
		return commitResolver.waitServiceResult(param);
	}
	
	
}

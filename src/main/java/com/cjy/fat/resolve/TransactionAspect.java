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
import com.cjy.fat.resolve.handler.ServiceRunningHandler;
import com.cjy.fat.resolve.register.RedisRegister;

@Aspect
@Component
@ConditionalOnClass(value = { DataSourceTransactionManager.class })
@Order(36)
public class TransactionAspect {
	
	@Autowired
	RedisRegister redisRegister;

	@Autowired
	ServiceRunningHandler serviceHandler;
	
	@Autowired
	CommitResolver commitResolver;

	@Pointcut("@annotation(fatTransaction)")
	public void fatTransaction(FatTransaction fatTransaction) {

	}

	@Around("fatTransaction(fatTransaction)")
	public Object doAround(JoinPoint joinPoint, FatTransaction fatTransaction) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		ProceedingJoinPoint proceedingJoinPoint = (ProceedingJoinPoint) joinPoint;
		
		if(StringUtils.isBlank(TransactionContent.getRootTxKey())){
			return proceedingJoinPoint.proceed();
		}
		
		Method serviceMethod = signature.getMethod();
		Transactional transactionalAnno = serviceMethod.getAnnotation(Transactional.class);
		if(null == transactionalAnno) {
			throw new FatTransactionException("the method " + serviceMethod.getName() + " is not decorated by @Transactional");
		}
		
		String localTxMark = TransactionContent.getServiceId() + "-" + serviceMethod.getName();
		TransactionResolveParam txParam = TransactionResolveParam.buildTxParam(localTxMark);
		redisRegister.opsForGroupServiceSetOperation().addToGroupServiceSet(localTxMark);
		
		// 异步执行业务操作
		serviceHandler.proceed(proceedingJoinPoint , transactionalAnno, txParam ,TransactionContent.getRootTxKey());
		return commitResolver.waitServiceResult(txParam);
	}
	
	
}

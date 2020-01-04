package com.cjy.fat.resolve.handler;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.data.TransactionResolveParam;
import com.cjy.fat.exception.FatTransactionException;
import com.cjy.fat.resolve.CommitResolver;
import com.cjy.fat.resolve.register.ServiceRegister;

@Service
@ConditionalOnClass(value= {DataSourceTransactionManager.class})
public class ServiceRunningHandler {
	
	@Autowired
	ApplicationContext context;
	
	private static final Logger Logger = LoggerFactory.getLogger(ServiceRunningHandler.class);

	@Autowired
	ServiceRegister register;
	
	@Autowired
	CommitResolver commitResolver;
	
	@Async("fatTxExecutor")
	public void proceed(ProceedingJoinPoint joinPoint,Transactional transactional ,TransactionResolveParam txParam) throws Throwable {
		TransactionContent.buildContainer(txParam);
		
		AbstractPlatformTransactionManager transactionManager = null ;
		String transactionManagerName = transactional.transactionManager();
		if(StringUtils.isNotBlank(transactionManagerName)) {
			transactionManager = (AbstractPlatformTransactionManager) context.getBean(transactionManagerName);
		}else {
			transactionManager = context.getBean(DataSourceTransactionManager.class);
		}
		DefaultTransactionDefinition transDefinition = new DefaultTransactionDefinition();
		transDefinition.setIsolationLevel(transactional.isolation().value());
		transDefinition.setPropagationBehavior(transactional.propagation().value());
		transDefinition.setReadOnly(transactional.readOnly());
		transDefinition.setTimeout(transactional.timeout());
		TransactionStatus transStatus = transactionManager.getTransaction(transDefinition);
		
		try {
			Logger.info(txParam.getLocalTxMark() + " transaction start" );
			
			register.opsForServiceError().isServiceError();
			
			Object result = joinPoint.proceed();
			
			txParam.setLocalRunningResult(result);
			
			Logger.info(txParam.getLocalTxMark() + " is finished ,transaction is waiting for commit");
			
			commitResolver.blockProceed(txParam);
			
			transactionManager.commit(transStatus);
			
			Logger.info(txParam.getLocalTxMark() +  " transaction commit");
			
		} catch (Exception e) {
			
			if(!FatTransactionException.isFatException(e)) {
				register.opsForServiceError().serviceError(txParam.getLocalTxMark());
			}
			
			txParam.setLocalRunningException(e);
			
			transactionManager.rollback(transStatus);
			
			Logger.info(txParam.getLocalTxMark() +  " rollbarck");
			
			e.printStackTrace();
		}
	}

}

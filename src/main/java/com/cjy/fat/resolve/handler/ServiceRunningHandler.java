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
import com.cjy.fat.redis.RedisHelper;
import com.cjy.fat.resolve.CommitResolver;

@Service
@ConditionalOnClass(value= {DataSourceTransactionManager.class})
public class ServiceRunningHandler {
	
	@Autowired
	ApplicationContext context;
	
	private static final Logger Logger = LoggerFactory.getLogger(ServiceRunningHandler.class);

	@Autowired
	RedisHelper redisHelper;
	
	@Autowired
	CommitResolver commitResolver;
	
	@Async("transactionResolveExecutor")
	public void proceed(ProceedingJoinPoint joinPoint,Transactional transactional ,TransactionResolveParam param ,String rootTxKey) throws Throwable {
		this.bulidCunrrentThreadTxAttributesContent(rootTxKey);
		
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
			Logger.info(param.getLocalTxMark() + " transaction start" );
			
			redisHelper.opsForServiceError().isServiceError();
			
			Object result = joinPoint.proceed();
			// 写入执行结果返回主线程 ,改用本地阻塞式队列
			param.offerToLocalResultQueue(result);
			
			Logger.info(param.getLocalTxMark() + " is finished , transaction is waiting for commit");
			
			// 交给事务提交处理器处理可提交逻辑
			commitResolver.blockProceed(param);
			
			transactionManager.commit(transStatus);
			
			Logger.info( param.getLocalTxMark() +  " transaction commit");
			
		} catch (Exception e) {
			redisHelper.opsForServiceError().serviceError();
			param.setLocalRunningException(e);
			transactionManager.rollback(transStatus);
			throw new Exception( param.getLocalTxMark() + " transaction rollback" ,e);
		}
	}
	
	/**
	 * 由于在服务调服务的场景中，可能出现在service方法调用远程服务，处于不同线程中，所以此时需要再一次初始化当前线程的container
	 * @param txData
	 */
	private void bulidCunrrentThreadTxAttributesContent(String rootTxKey) {
		if(StringUtils.isNotBlank(rootTxKey)) {
			TransactionContent.initContainer();
			TransactionContent.setRootTxKey(rootTxKey);
		}
	}

}

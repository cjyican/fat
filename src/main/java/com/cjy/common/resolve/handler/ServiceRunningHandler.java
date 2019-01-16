package com.cjy.common.resolve.handler;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.alibaba.fastjson.JSONObject;
import com.cjy.common.data.TxAttributesContent;
import com.cjy.common.data.TxResolveParam;
import com.cjy.common.redis.TxRedisHelper;
import com.cjy.common.resolve.TxCommitResolver;

@Service
@ConditionalOnClass(value= {DataSourceTransactionManager.class})
public class ServiceRunningHandler {
	
	private static final Logger Logger = LoggerFactory.getLogger(ServiceRunningHandler.class);

	@Autowired
	DataSourceTransactionManager transactionManager;

	@Autowired
	TxRedisHelper txRedisHelper;
	
	@Autowired
	TxCommitResolver commitResolver;
	
	@Async("transactionResolveExecutor")
	public void proceed(ProceedingJoinPoint joinPoint,Transactional transactional ,TxResolveParam param ,Map<String , String> txData) throws Throwable {
		this.bulidCunrrentThreadTxAttributesContent(txData);
		DefaultTransactionDefinition transDefinition = new DefaultTransactionDefinition();
		transDefinition.setIsolationLevel(transactional.isolation().value());
		transDefinition.setPropagationBehavior(transactional.propagation().value());
		transDefinition.setReadOnly(transactional.readOnly());
		transDefinition.setTimeout(transactional.timeout());
		TransactionStatus transStatus = transactionManager.getTransaction(transDefinition);
		try {
			Logger.info("{}-transaction start"  ,param.getLocalTxMark());
			txRedisHelper.opsForServiceError().isTxServiceError(param.getTxKey());
			Object result = joinPoint.proceed();
			String resultJSON = JSONObject.toJSONString(result);
			Logger.info("{}-service is finished , transaction is waiting for commit,service result:{}", param.getLocalTxMark(), resultJSON);
			// 写入执行结果，提供给其他服务的参数使用 ,改用本地阻塞式队列
			param.offerToLocalResultQueue(resultJSON);
			// 交给事务提交处理器处理可提交逻辑
			commitResolver.blockProceed(param);
			transactionManager.commit(transStatus);
			Logger.info("{}-transaction commit" , param.getLocalTxMark());
		} catch (Exception e) {
			txRedisHelper.opsForServiceError().txServiceError(param.getTxKey());
			Logger.error("{}-transaction rollback ,error:{}", param.getLocalTxMark() , e.getMessage());
			transactionManager.rollback(transStatus);
			e.printStackTrace();
		}
	}
	
	/**
	 * 由于在服务调服务的场景中，可能出现在service方法调用远程服务，处于不同线程中，所以此时需要再一次初始化当前线程的container
	 * @param txData
	 */
	private void bulidCunrrentThreadTxAttributesContent(Map<String , String> txData) {
		if(null != txData) {
			TxAttributesContent.initContainer();
			String rootTxKey = txData.get(TxAttributesContent.STR_ROOT_TX_KEY);
			String localTxKey = txData.get(TxAttributesContent.STR_REMOTE_TX_KEY);
			if(StringUtils.isNotBlank(rootTxKey)) {
				TxAttributesContent.setRootTxKey(rootTxKey);
			}
			if(StringUtils.isNotBlank(localTxKey)) {
				TxAttributesContent.setLocalTxKey(localTxKey);
			}
		}
	}

}

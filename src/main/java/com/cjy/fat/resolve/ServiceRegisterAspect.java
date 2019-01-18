package com.cjy.fat.resolve;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.cjy.fat.annotation.FatServiceRegister;
import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.exception.FatTransactionException;
import com.cjy.fat.redis.RedisHelper;
import com.cjy.fat.redis.constant.RedisKeyEnum;
import com.cjy.fat.resolve.accept.RemoteTransactionDataResolver;

@Aspect
@Component
@Order(24)
public class ServiceRegisterAspect {
	
	private static final Logger Logger = LoggerFactory.getLogger(ServiceRegisterAspect.class);
	
	@Autowired
	RedisHelper redisHelper;

	@Value("${spring.application.name}")
	String serviceName;
	
	@Autowired
	CommitResolver commitResolver;
	
	@Autowired
	ServiceRegisterResolver serviceRegister;
	
	@Autowired
	RemoteTransactionDataResolver remoteTransactionDataHelper;

	@Pointcut("@annotation(txRegisterService)")
	public void txServiceRegister(FatServiceRegister txRegisterService) {

	}

	//入口开始执行服务注册时，判断该接口是不是一个服务，包含本地事务与调用其他服务，使用Request.remoteTxKey判断。
	//当remoteTxKey存在的时候，说明该接口作为一个服务提供，且包含本地事务与调用其他服务。需要进行事务分组。
	//再次生成LocalTxKey作为本地/调用其他服务的分布式事务管理。
	//该服务的本地事务与调用其它的服务作为上层事务的一个子事务，当子事务完成操作，可以预备提交时。
	//该接口需要挂起子事务组，返回子事务组操作完毕的标识给上层的父事务，当父事务开始提交时，子事务组监听，一起进行提交
	@Before("txServiceRegister(txRegisterService)")
	public void doBefore(JoinPoint joinPoint, FatServiceRegister txRegisterService) {
		remoteTransactionDataHelper.init();
		serviceRegister.registerService(txRegisterService);
	}

	@AfterThrowing(value="txServiceRegister(txRegisterService)" , throwing = "ex")
	public void handleThrowing(JoinPoint joinPoint, FatServiceRegister txRegisterService , Exception ex ) {
		String localTxkey = TransactionContent.getLocalTxKey();
		String remoteTxKey = TransactionContent.getRemoteTxKey();
		if(StringUtils.isNotBlank(localTxkey)){
			// 写入错误标识，引发回滚本地事务/子事务组
			redisHelper.opsForServiceError().serviceError(localTxkey);
			redisHelper.opsForBlockMarkOperation().unPassBlockMark(localTxkey, RedisKeyEnum.SERVICE_READYCOMMIT_MARK);
		}
		if(StringUtils.isNotBlank(remoteTxKey)){
			// 写入错误标识，引发回滚父事务组
			redisHelper.opsForServiceError().serviceError(remoteTxKey);
			redisHelper.opsForBlockMarkOperation().unPassBlockMark(remoteTxKey, RedisKeyEnum.SERVICE_READYCOMMIT_MARK);
		}
		Logger.error(ex.getMessage());
	}

	//客户端不需要等待结果，服务等待结果返回给客户端即可。
	@AfterReturning(value = "txServiceRegister(txRegisterService)", returning = "sourceResult")
	public void doAfterReturn(JoinPoint joinPoint, Object sourceResult, FatServiceRegister txRegisterService)
			throws Exception {
		String localTxKey = TransactionContent.getLocalTxKey();
		String remoteTxKey = TransactionContent.getRemoteTxKey();
		String rootTxKey = TransactionContent.getRootTxKey();
		String serviceId = TransactionContent.getServiceId();

		// 当存在远程事务 ， 本地事务时需要明确指定本地事务数量 , 若此时本地事务组依然存在元素，说明数量配置不正确
		if(txRegisterService.localTransactionCount() > 0){
			if(TransactionContent.localTxQueueSize() > 0){
				throw new FatTransactionException(localTxKey , "local transaction count is incorrect , which is more than real count" ); 
			}
		}
		commitResolver.clientProcced(txRegisterService, remoteTxKey, localTxKey, rootTxKey, serviceId);
	}

}

package com.cjy.fat.resolve;

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
import com.cjy.fat.resolve.register.redis.RedisRegister;

@Aspect
@Component
@Order(24)
public class ServiceRegisterAspect {
	
	private static final Logger Logger = LoggerFactory.getLogger(ServiceRegisterAspect.class);
	
	@Autowired
	RedisRegister redisRegister;

	@Value("${spring.application.name}")
	String serviceName;
	
	@Autowired
	CommitResolver commitResolver;
	
	@Autowired
	ServiceRegisterResolver serviceRegister;
	
	@Autowired
	RemoteTransactionDataResolver remoteTransactionDataResolver;

	@Pointcut("@annotation(txRegisterService)")
	public void txServiceRegister(FatServiceRegister txRegisterService) {

	}

	@Before("txServiceRegister(txRegisterService)")
	public void doBefore(JoinPoint joinPoint, FatServiceRegister txRegisterService) {
		remoteTransactionDataResolver.init();
		serviceRegister.registerService(txRegisterService);
	}

	@AfterThrowing(value="txServiceRegister(txRegisterService)" , throwing = "ex")
	public void handleThrowing(JoinPoint joinPoint, FatServiceRegister txRegisterService , Exception ex ) {
		redisRegister.opsForServiceError().serviceError();
		Logger.error(ex.getMessage());
	}

	@AfterReturning(value = "txServiceRegister(txRegisterService)")
	public void doAfterReturn(JoinPoint joinPoint, FatServiceRegister txRegisterService)
			throws Exception {
		commitResolver.clientProcced();
	}

}

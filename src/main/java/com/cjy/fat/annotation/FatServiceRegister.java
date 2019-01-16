package com.cjy.fat.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启分布式事务的客户端标识
 * @author CJY
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface FatServiceRegister {
	
	/**
	 * 需要进入加入事务的service
	 * @return
	 */
	@Deprecated
	Class<?>[] value() default {};
	
	/**
	 * 业务线处理完成时间总等待时长 , 默认5秒
	 */
	@Deprecated
	long waitFinishMilliesSeconds() default 5000;
	
	/**
	 * 本地事务数量
	 */
	int localTransactionCount() default 0; 
	
	/**
	 * 服务数量
	 * A-->B,A-->C ;A.serviceCount==2,B/C.serviceCoun==0
	 * A-->B,A-->B ;A.serviceCount==2,B.serviceCoun==0
	 * A-->B,B-->C ;A.serviceCont==1,B.serviceCont==1,C.serviceCount==0
	 */
	int serviceCount() default 0;
}

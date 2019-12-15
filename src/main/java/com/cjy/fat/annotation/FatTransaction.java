package com.cjy.fat.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启分布式事务的Service业务方法
 * @author CJY
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface FatTransaction {
	
	/**
	 * 等待当前服务返回值的超时时间 , 默认3秒
	 */
	long waitResultMillisSeconds() default 3000;
	
	/**
	 * 服务等待提交时间 ,默认3秒
	 */
	long waitCommitMillisSeconds() default 3000; 
	
	
}

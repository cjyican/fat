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
	 * 是否等待当前服务的返回值 ,默认等待。
	 * 设置为不等待可以提高一定的性能，但是必须要保证该方法的返回值具有默认的构造器。
	 * @return
	 */
	@Deprecated
	boolean waitResult() default true;
	
	/**
	 * 等待当前服务返回值的超时时间 , 默认3秒
	 */
	long waitResultMillisSeconds() default 3000;
	
	/**
	 * 服务等待提交时间 ,默认3秒
	 */
	long waitCommitMillisSeconds() default 3000; 
	
	
}

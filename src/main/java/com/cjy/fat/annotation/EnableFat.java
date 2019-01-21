package com.cjy.fat.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.cjy.fat.FatAutoConfigeration;

/**
 * 是否开启Fat分布式事务管理
 * @author cjy
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(FatAutoConfigeration.class)
@Documented
@Inherited
public @interface EnableFat {

}

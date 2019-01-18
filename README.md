# fat
基于springboot , 使用redis , spring async , spring transactionManager的强一致性分布式事务解决方案
## 项目介绍
使用redis作为注册中心 ,spring async异步处理事务。<br>
纯编码方式，强一致性。<br>
基于注解使用，对业务代码可以说是零入侵，目前内置适配spring-cloud(Feign调用) ， dubbo。<br>
同时具备强大扩展性，因为存在自定义的服务框架，或者以后会涌现出更多的流行服务框架，所以会提供一些组件适配自定义服务框架。

## Maven依赖
```java
<dependency>
    <groupId>com.github.cjyican</groupId>
    <artifactId>fat-common</artifactId>
    <version>1.0.0-RELEASE</version>
</dependency>
```
## 使用示例
### step1:配置注册中心
使用redis作为注册中心，所以需要引入配置redis，暂未适配集群模式。为隔离业务使用的redis和注册中心的redis，提供了一套属性配置。
在业务redis与注册中心相同时，也需要配置。
请保证各个服务的注册中心配置一致，否则无法协调分布式事务。
```java
#Fat
# Redis数据库索引（默认为0）
fat.redis.database=0
# Redis服务器地址
fat.redis.host=
# Redis服务器连接端口
fat.redis.port=6379
# Redis服务器连接密码（默认为空）
fat.redis.password=
# 连接池最大连接数（使用负值表示没有限制）
fat.redis.pool.max-active=20
# 连接池最大阻塞等待时间（使用负值表示没有限制）
fat.redis.pool.max-wait=-1
# 连接池中的最大空闲连接
fat.redis.pool.max-idle=10
# 连接池中的最小空闲连接
fat.redis.pool.min-idle=2
# 连接超时时间（毫秒）
fat.redis.timeout=1000 
```
### step2:服务接口加入注解@FatServiceRegister注册
在需要开启分布式事务管理的接口/方法中加入注解@FatServiceRegister，注意不要重复添加。dubbo的直接加在service.method上面就可以了。
```java
@RequestMapping("/user-service/{userId}/updateUserOrderNum1")
@FatServiceRegister(serviceCount = 1 , localTransactionCount = 1)
public Integer updateUserOrderNum1(@PathVariable("userId") Long userId , @RequestParam("lastOrderId") Long lastOrderId ) throws Exception {
  int userResult = service.updateUserOrderNum(userId , lastOrderId);
  prodFeign.updateStock(1l);
//int i = 10 / 0;测试使用
  return userResult;
}
```
注解解析
```java
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
```
### step3:业务方法加注解@FatTransaction纳入分布式管控
注意@FatTransaction必须要与@Transactional配合使用已获取用户配置的事务信息，否则将会报错
```java
@FatTransaction
@Transactional
public Integer updateUserOrderNum(Long userId ,Long lastOrderId ) throws Exception {
  User user = new User();
  user.setLastOrderId(lastOrderId);
  user.setUserId(userId);
  int i = mapper.updateUserOrderNum(user);
//int j = 10 /0 ;//测试使用
  return i;
}
```
注解解析
```java
/**
 * 等待当前服务返回值的超时时间 , 默认3秒
 */
long waitResultMillisSeconds() default 3000;

/**
 * 服务等待提交时间 ,默认3秒
 */
long waitCommitMillisSeconds() default 3000; 
```
OK,到这里这个接口的服务链路已经完成了，可以跑起来了。简单吧，嘿嘿嘿。

## 原理解析

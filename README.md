# fat
FAT ,基于springboot , 使用redis , spring async , spring transactionManager的强一致性分布式事务解决方案
## 框架介绍
使用redis作为注册中心 ,手动管理事务的执行，spring async异步处理事务。<br>
纯编码方式，强一致性。<br>
基于注解使用，对业务代码可以说是零入侵，目前内置适配spring-cloud(Feign调用) ， dubbo。<br>
同时具备一定的扩展性与兼容性，因为存在自定义的服务框架，或者以后会涌现出更多的流行服务框架，所以会提供一些组件适配自定义服务框架。

## Maven依赖
```java
<dependency>
    <groupId>com.github.cjyican</groupId>
    <artifactId>fat-common</artifactId>
    <version>1.0.2-RELEASE</version>
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
应用标识，与spirng.application.name一致，必须配置
```java
spring.application.name=fatboy-eureka-ribbon
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
## 运行流程
![头像](https://github.com/cjyican/img-respo/blob/master/TIM20190118153529.png)
<br>图不重要，重要的思想和代码，下面介绍一下FAT的一些设计和源码

## 设计与源码解析
直接看代码，注释非常清晰<br>
主要处理流程都集中在<br>
https://github.com/cjyican/fat/tree/master/src/main/java/com/cjy/fat/resolve
### 注册流程
https://github.com/cjyican/fat/blob/master/src/main/java/com/cjy/fat/resolve/ServiceRegisterAspect.java<br>
https://github.com/cjyican/fat/blob/master/src/main/java/com/cjy/fat/resolve/ServiceRegisterResolver.java<br>
### 业务方法流程
https://github.com/cjyican/fat/blob/master/src/main/java/com/cjy/fat/resolve/TransactionAspect.java<br>
https://github.com/cjyican/fat/blob/master/src/main/java/com/cjy/fat/resolve/handler/ServiceRunningHandler.java<br>
### 事务监听提交流程
https://github.com/cjyican/fat/blob/master/src/main/java/com/cjy/fat/resolve/CommitResolver.java<br>

## 可自定义的配置
### 事务处理的线程池
FAT使用Sping Async处理事务流程，自然需要用到线程池，线程池默认有配置，也可以根据项目运行情况自定义，以下为配置信息
```java
@Value("${fb.thread.core_pool_size:20}")
private int corePoolSize ;

@Value("${fb.thread.max_pool_size:50}")
private int maxPoolSize ;

@Value("${fb.thread.queue_capacity:200}")
private int queueCapacity ;

@Value("${fb.thread.keep_alive_seconds:60}")
private int keepAliveSeconds;
```
### 监听事务执行情况，业务结果的间歇时间
根据项目运行情况配置，默认0.2秒
```java
/**
 * 间歇消费时间（毫秒）默认200毫秒
 * 争抢可提交标识的时候，可能发生错误，避免继续阻塞，导致jdbcConnection/数据库事务迟迟不肯放手，为了提高响应速度，
 * 将pop的阻塞时间分段请求
 */
@Value("${tx.commit.blankTime:200}")
private long commitBlankTime ;

@Value("${tx.waitResult.blankTime:200}")
private long waitResultBlankTime;
```

## 扩展
以后应该会有更多的流行服务框架，所以一个个适配是不可能的了，这辈子都不可能出一个适配一个的了(开玩笑的，希望我的设计可以兼容吧)<br>
### step1:FAT提供分布式事务上下文获取方法
```java
/**
 * 获取事务信息提供给自定义拦截器
 * @return
 */
public static final Map<String , String> buildRemoteData(){
    //返回新的对象，不开放修改入口，避免被客户端串改
    Map<String , String> map = new HashMap<>();
    map.put(STR_REMOTE_TX_KEY, getLocalTxKey());
    map.put(STR_ROOT_TX_KEY, getRootTxKey());
    return map;
}
```
自定义服务框架，需要在调用服务时，把这个map数据包的key-value传达到服务上下文。
例如，dubbo的自定义SPI-Filter,需要遍历此map，RpcContext.getContext().setAttachment(key,value)

### step2:实现CustomRemoteDataAdapter接口，为FAT提供服务上下文的数据源
自定义服务框架需要提供该接口，并需要加入到spring上下文中，让FAT获取到数据源，进而把分布式上下文信息传播到本地
```java
public interface CustomRemoteDataAdapter {
	
	/**
	 * eg:dubbo:RpcContent.getAttachments();
	 * @return
	 */
	Map<String , String> convertRemoteDataToMap();
	
}
```
例如Dubbo
```java
@Compoent
public Class DubboRemoteDataAdapter implements CustomRemoteDataAdapter{
	
	/**
	 * eg:dubbo:RpcContent.getAttachments();
	 * 在github的readme.md写代码是一种怎么样的体验
	 */
	public Map<String , String> convertRemoteDataToMap(){
        	return RpcContent.getAttachments();
   	}
}
```

## 后续更新
1，会持续更新维护
2，打造FAT分布式事务管控平台FAT-monitor


## 结语
（我前天才注册的gay佬hub...）<br>
FAT是我第一次学java来投入如此大心血写的框架，也是我职业生涯第一个开源作品吧，不论好与坏，我都为之自豪，成就感爆棚。<br>
迫不及待想与大家分享，如同小孩吃糖一般<br>
希望可以相互学习，互相交流!<br>
thank you for your star.

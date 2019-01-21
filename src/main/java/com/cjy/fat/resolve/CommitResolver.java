package com.cjy.fat.resolve;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.cjy.fat.annotation.FatServiceRegister;
import com.cjy.fat.data.TransactionResolveParam;
import com.cjy.fat.exception.FatTransactionException;
import com.cjy.fat.redis.RedisHelper;
import com.cjy.fat.redis.constant.RedisKeyEnum;

@Component
public class CommitResolver {

	@Autowired
	RedisHelper redisHelper;

	/**
	 * 间歇消费时间（毫秒）默认200毫秒
	 * 争抢可提交标识的时候，可能发生错误，避免继续阻塞，导致jdbcConnection/数据库事务迟迟不肯放手，为了提高响应速度，
	 * 将pop的阻塞时间分段请求
	 */
	@Value("${tx.commit.blankTime:200}")
	private long commitBlankTime ;
	
	@Value("${tx.waitResult.blankTime:200}")
	private long waitResultBlankTime;
	
	/**
	 * 阻塞式提交 , 用于事务
	 * 
	 * @param param
	 */
	public void blockProceed(TransactionResolveParam param) {
		// 记录业务完成 ， 开始尝试提交事务时间戳，事务提交需要反向提交，越迟尝试提交的越早提交
		redisHelper.opsForServiceFinishTimeZsetOperation().addServiceFinishZSet(param.getTxKey(), param.getLocalTxMark());
		// 判断是否所有服务都已经完成业务, 改用redis阻塞等待机制 , (使用线程变量)
		if (redisHelper.opsForServiceFinishTimeZsetOperation().isServiceFinishZSetFull(param.getTxKey())) {
//			// 将serviceSet的元素加入到readCommitList中
			this.passServiceReadyCommitList(param.getTxKey());
		}
		// 争抢预备提交位(此时其他服务增在执行业务，最后完成业务的将会存入往预备提交队列存入serviceZet)
		// 这个更多的意义在于让各事务的起步时间同步，降低时间差导致的提交不一致情况
		this.popServiceReadyCommitList(param.getTxKey(),  param.getLocalTxMark(),  param.getWaitCommitMilliesSeconds());
		redisHelper.opsForGroupFinishSetOperation().addGroupFinishSet(param.getRootTxKey(), param.getTxKey());
		// 当事务分组协调器维护的txkey数量等于完成数量的时候 ， 告诉各localTxKey可以提交
		if(redisHelper.opsForGroupFinishSetOperation().isGroupFinishZSetFull(param.getRootTxKey())) {	
			this.passGroupCancommitList(param.getRootTxKey());
		}
		// 争抢分组管理器提交位
		this.popGroupCanCommitList(param.getRootTxKey(), param.getTxKey(),  Integer.MAX_VALUE);
	}
	
	public void passServiceReadyCommitList(String txKey) {
		// 将serviceSet的元素加入到readCommitList中
		redisHelper.opsForServiceReadyCommitListOperation().pushServiceSetToReadCommitList(txKey);
		// 写入passed，后续block操作直接读标志位
		redisHelper.opsForBlockMarkOperation().passBlockMark(txKey, RedisKeyEnum.SERVICE_READYCOMMIT_MARK);
	}
	
	public void passServiceCancommitList(String txKey) {
		// 将serviceSet的元素加入到CancommitList中
		redisHelper.opsForServiceCancommitListOperation().pushServiceSetToCancommitList(txKey);
		// 写入passed，后续block操作直接读标志位
		redisHelper.opsForBlockMarkOperation().passBlockMark(txKey, RedisKeyEnum.SERVICE_CANCOMMIT_MARK);
	}
	
	public void passGroupCancommitList(String rootTxKey) {
		redisHelper.opsForGroupCanCommitListOperation().pushGroupServiceSetToGroupCommitList(rootTxKey);
		// 写入passed，后续block操作直接读标志位
		redisHelper.opsForBlockMarkOperation().passBlockMark(rootTxKey, RedisKeyEnum.GROUP_CANCOMMIT_MARK);
	}
	
	public boolean popServiceReadyCommitList(String txKey , String content , long waitMilliesSeconds) {
		return popCommitListWithBlankTime(RedisKeyEnum.SERVICE_READYCOMMIT_LIST, RedisKeyEnum.SERVICE_READYCOMMIT_MARK, txKey, content, waitMilliesSeconds);
	}
	
	public boolean popServiceCanCommitList(String txKey , String content , long waitMilliesSeconds) {
		return popCommitListWithBlankTime(RedisKeyEnum.SERVICE_CANCOMMIT_LIST, RedisKeyEnum.SERVICE_CANCOMMIT_MARK, txKey, content, waitMilliesSeconds);
	}
	
	public boolean popGroupCanCommitList(String rootTxKey , String content , long waitMilliesSeconds) {
		return popCommitListWithBlankTime(RedisKeyEnum.GROUP_CANCOMMIT_LIST, RedisKeyEnum.GROUP_CANCOMMIT_MARK, rootTxKey, content, waitMilliesSeconds);
	}
	
	/**
	 * 监听提交队列
	 * @param keyEnum 针对的redis_key 哪个提交队列
	 * @param txKey 
	 * @param content 写入阻塞队列的内容
	 * @param waitMilliesSeconds 阻塞总时长
	 * @return 这个方法玩的是阻塞，返回值暂时没用的
	 */
	public boolean popCommitListWithBlankTime(RedisKeyEnum keyEnum ,RedisKeyEnum markEnum , String txKey , String content , long waitMilliesSeconds ) {
		long popTimes = getTryTimes(waitMilliesSeconds, commitBlankTime);
		for (int i = 0; i < popTimes; i++) {
			//先检测是否出错
			redisHelper.opsForServiceError().isServiceError(txKey);
			boolean isPassed = redisHelper.opsForBlockMarkOperation().isBlockMarkPassed(txKey, markEnum);
			if(isPassed) {
				return true;
			}
			// 争抢预备提交位(此时其他服务增在执行业务，最后完成业务的将会存入一个预备提交位)
			String canCommit = redisHelper.popBlockList(txKey, keyEnum, commitBlankTime);
			if(StringUtils.isNotBlank(canCommit)){
				return true;
			}
		}
		throw new FatTransactionException(txKey, txKey+" local service is finished , wait for commit timeout");
	}
	
	private long getTryTimes(long waitMilliesSeconds , long blankTime) {
		long popTimes = waitMilliesSeconds / blankTime;
		popTimes = waitMilliesSeconds % blankTime > 0 ? popTimes + 1 : popTimes;
		return popTimes;
	}
	
	/**
	 * 客户端提交过程
	 * @param param
	 */
	public void clientProcced(FatServiceRegister txRegisterService, String remoteTxKey , String localTxKey ,String rootTxKey , String serviceId ){
		//先通知父事务组
		if(StringUtils.isNotBlank(remoteTxKey)){
			this.clientCommonProcced(remoteTxKey, rootTxKey, serviceId);
		}
		
		//父事务组开始进行事务提交的时候，本事务组也开始进行提交
		if(StringUtils.isNotBlank(localTxKey)){
			this.clientCommonProcced(localTxKey, rootTxKey, serviceId);
		}
	}
	
	private void clientCommonProcced(String txKey , String rootTxKey , String serviceId) {
		redisHelper.opsForServiceError().isServiceError(txKey);
		// 当前事务组完成时间 , 因为存在本地事务与服务调服务的时候，该协调交给发起者处理，不在ServieRunningHandler处理了
		redisHelper.opsForServiceFinishTimeZsetOperation().addServiceFinishZSet(txKey, serviceId);
		// 判断是否所有服务都已经完成业务, 改用redis阻塞等待机制
		if(redisHelper.opsForServiceFinishTimeZsetOperation().isServiceFinishZSetFull(txKey)) {
			// 将serviceSet的元素加入到readCommitList中
			this.passServiceReadyCommitList(txKey);
			redisHelper.opsForGroupFinishSetOperation().addGroupFinishSet(rootTxKey, txKey);
		}
	}
	
	/**
	 * 本地BlockQueue获取服务执行结果
	 * @param param
	 * @return
	 * @throws InterruptedException
	 */
	public Object waitServiceResult(TransactionResolveParam param) throws Exception {
		String serviceResult = null;
		long tryTimes = getTryTimes(param.getWaitResultMilliesSeconds(), waitResultBlankTime);
		for(int i = 0 ; i < tryTimes ; i++) {
			//检查是否事务出错
			if(null != param.getLocalRunningException()) {
				throw param.getLocalRunningException();
			}
			serviceResult = param.pollFromLocalResultQueue(waitResultBlankTime);
			if(StringUtils.isNotBlank(serviceResult)) {
				break;
			}
		}
		//等待超时，查看是否可以阻止事务提交
		if(StringUtils.isBlank(serviceResult)) {
			boolean isPassed = redisHelper.opsForBlockMarkOperation().isBlockMarkPassed(param.getTxKey(), RedisKeyEnum.SERVICE_READYCOMMIT_MARK);
			if(!isPassed) {
				throw new FatTransactionException(param.getTxKey(), "wait result time out , transaction roll back ");
			}
			//不能阻止事务提交，进入等待
			for(;;) {
				//检查是否事务出错
				redisHelper.opsForServiceError().isServiceError(param.getTxKey());
				serviceResult = param.pollFromLocalResultQueue(waitResultBlankTime);
				if(StringUtils.isNotBlank(serviceResult)) {
					break;
				}
			}
		}
		return JSONObject.parseObject(serviceResult, param.getReturnType());
	}

}

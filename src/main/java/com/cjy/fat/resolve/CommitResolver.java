package com.cjy.fat.resolve;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
	 * 争抢可提交标识的时候，可能发生错误，避免继续阻塞，导致jdbcConnection/数据库事务迟迟不肯放手，提高响应速度，
	 * 将pop的阻塞时间分段请求
	 */
	@Value("${tx.commit.blankTime:100}")
	private long commitBlankTime ;
	
	@Value("${tx.waitResult.blankTime:100}")
	private long waitResultBlankTime;
	
	/**
	 * 阻塞式提交 , 用于事务
	 * 
	 * @param param
	 */
	public void blockProceed(TransactionResolveParam param) {
		
		redisHelper.opsForGroupFinishSetOperation().addGroupFinishSet(param.getRootTxKey(), param.getLocalTxMark());
		
		// 当事务分组协调器维护的txkey数量等于完成数量的时候 ， 告诉各localTxKey可以提交
		if(redisHelper.opsForGroupFinishSetOperation().isGroupFinishZSetFull(param.getRootTxKey())) {	
			this.passGroupCancommitList(param.getRootTxKey());
		}
		
		// 争抢分组管理器提交位
		this.popGroupCanCommitList(param.getRootTxKey(), param.getLocalTxMark(),  param.getWaitCommitMilliesSeconds());
	}
	
	public void passGroupCancommitList(String rootTxKey) {
		redisHelper.opsForGroupCanCommitListOperation().pushGroupServiceSetToGroupCommitList(rootTxKey);
		// 写入passed，后续block操作直接读标志位
		redisHelper.opsForBlockMarkOperation().passBlockMark(rootTxKey, RedisKeyEnum.GROUP_CANCOMMIT_MARK);
	}
	
	public boolean popGroupCanCommitList(String rootTxKey , String content , long waitMilliesSeconds) {
		return popCommitListWithBlankTime(RedisKeyEnum.GROUP_CANCOMMIT_LIST, RedisKeyEnum.GROUP_CANCOMMIT_MARK, rootTxKey, waitMilliesSeconds);
	}
	
	/**
	 * 监听提交队列
	 * @param keyEnum 针对的redis_key 哪个提交队列
	 * @param txKey 
	 * @param content 写入阻塞队列的内容
	 * @param waitMilliesSeconds 阻塞总时长
	 * @return 这个方法玩的是阻塞，返回值暂时没用的
	 */
	public boolean popCommitListWithBlankTime(RedisKeyEnum keyEnum ,RedisKeyEnum markEnum , String txKey, long waitMilliesSeconds ) {
//		long popTimes = getTryTimes(waitMilliesSeconds, commitBlankTime);
		long endTime = System.currentTimeMillis() + waitMilliesSeconds;
		while(System.currentTimeMillis() < endTime) {
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
		throw new FatTransactionException(txKey, txKey+" local transaction is finished, wait for commit timeout");
	}
	
//	private long getTryTimes(long waitMilliesSeconds , long blankTime) {
//		long popTimes = waitMilliesSeconds / blankTime;
//		popTimes = waitMilliesSeconds % blankTime > 0 ? popTimes + 1 : popTimes;
//		return popTimes;
//	}
	
	/**
	 * 客户端提交过程
	 * @param param
	 */
	public void clientProcced(String rootTxKey , String serviceId ){
		if(StringUtils.isNotBlank(rootTxKey)){
			this.clientCommonProcced( rootTxKey, serviceId);
		}
	}
	
	private void clientCommonProcced(String rootTxKey , String serviceId) {
		redisHelper.opsForServiceError().isServiceError(rootTxKey);
		redisHelper.opsForGroupFinishSetOperation().addGroupFinishSet(rootTxKey, serviceId);
		// 当事务分组协调器维护的txkey数量等于完成数量的时候 ， 告诉各localTxKey可以提交
		if(redisHelper.opsForGroupFinishSetOperation().isGroupFinishZSetFull(rootTxKey)) {	
			this.passGroupCancommitList(rootTxKey);
		}
	}
	
	/**
	 * 本地BlockQueue获取服务执行结果
	 * @param param
	 * @return
	 * @throws InterruptedException
	 */
	public Object waitServiceResult(TransactionResolveParam param) throws Exception {
		Object serviceResult = null;
		long endTime = System.currentTimeMillis() + param.getWaitResultMilliesSeconds();
		while(System.currentTimeMillis() < endTime) {
//			for(int i = 0 ; i < tryTimes ; i++) {
			//检查是否事务出错
			if(null != param.getLocalRunningException()) {
				throw param.getLocalRunningException();
			}
			serviceResult = param.pollFromLocalResultQueue(waitResultBlankTime);
			if(null != serviceResult) {
				break;
			}
		}
		//等待超时，查看是否可以阻止事务提交
		if(null == serviceResult) {
			boolean isPassed = redisHelper.opsForBlockMarkOperation().isBlockMarkPassed(param.getRootTxKey(), RedisKeyEnum.GROUP_CANCOMMIT_MARK);
			if(!isPassed) {
				throw new FatTransactionException(param.getRootTxKey(), "wait result time out , transaction roll back ");
			}
			//不能阻止事务提交，进入等待
			while(true) {
				//检查是否事务出错
				redisHelper.opsForServiceError().isServiceError(param.getRootTxKey());
				serviceResult = param.pollFromLocalResultQueue(waitResultBlankTime);
				if(null != serviceResult) {
					break;
				}
			}
		}
		return serviceResult;
	}

}

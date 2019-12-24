package com.cjy.fat.resolve;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.data.TransactionResolveParam;
import com.cjy.fat.redis.RedisHelper;

@Component
public class CommitResolver {

	@Autowired
	RedisHelper redisHelper;

	/**
	 * 间歇消费时间（毫秒）默认200毫秒
	 * 争抢可提交标识的时候，可能发生错误，避免继续阻塞，导致jdbcConnection/数据库事务一直阻塞，提高响应速度，
	 * 将pop的阻塞时间分段请求
	 */
	@Value("${tx.commit.blankTime:100}")
	private long commitBlankTime ;
	
//	@Value("${tx.waitResult.blankTime:100}")
//	private long waitResultBlankTime;
	
	/**
	 * 阻塞式提交 , 用于事务
	 * 
	 * @param param
	 */
	public void blockProceed(TransactionResolveParam param) {
		
		redisHelper.opsForGroupFinishSetOperation().addToGroupFinishSet(param.getLocalTxMark());
		
		// 当事务分组协调器维护的txkey数量等于完成数量的时候 ， 告诉各localTxKey可以提交
		if(redisHelper.opsForGroupFinishSetOperation().isGroupFinishZSetFull()) {
			
			this.passGroupCancommitList();
			
		}
		
		// 争抢分组管理器提交位
		this.popGroupCanCommitList();
	}
	
	public void passGroupCancommitList() {
		redisHelper.opsForGroupCanCommitListOperation().pushGroupServiceSetToGroupCommitList();
//		// 写入passed，后续block操作直接读标志位
//		redisHelper.opsForBlockMarkOperation().passBlockMark();
	}
	
	public boolean popGroupCanCommitList() {
//		long endTime = System.currentTimeMillis() + waitMilliesSeconds;
		
		boolean alreadyCancommit = false;
		while(true) {

//			boolean isPassed = redisHelper.opsForBlockMarkOperation().isBlockMarkPassed();
//			if(isPassed) {
//				return true;
//			}
			
			if(!alreadyCancommit) {
				String canCommit = redisHelper.opsForGroupCanCommitListOperation().popGroupCancommit(commitBlankTime);
				
				if(StringUtils.isBlank(canCommit)) {
					
					redisHelper.opsForServiceError().isServiceError();
					
					continue;
				}
				
				alreadyCancommit = true;
				
			}
			
				
			//TODO 检查业务链路是否已经完成，可以提交
			if(!redisHelper.opsForMainThreadMarkOperation().isFinshed()) {
				
				continue;
				
			}
			
			return true;
			
		}
//		throw new FatTransactionException(TransactionContent.getRootTxKey(), " local transaction is finished, wait for commit timeout");
	}
	
	/**
	 * 客户端提交过程
	 * @param param
	 */
	public void clientProcced(){
//		redisHelper.opsForServiceError().isServiceError();
		redisHelper.opsForGroupFinishSetOperation().addToGroupFinishSet(TransactionContent.getServiceId());
		// 当事务分组协调器维护的txkey数量等于完成数量的时候 ， 告诉各localTxKey可以提交
		if(redisHelper.opsForGroupFinishSetOperation().isGroupFinishZSetFull()) {	
			this.passGroupCancommitList();
		}
		if(TransactionContent.isLeader()) {
			redisHelper.opsForMainThreadMarkOperation().setFinshed();
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
//		long endTime = System.currentTimeMillis() + param.getWaitResultMilliesSeconds();
		while(true) {
			//检查是否事务出错
			if(null != param.getLocalRunningException()) {
				throw param.getLocalRunningException();
			}
//			serviceResult = param.pollFromLocalResultQueue(waitResultBlankTime);
			serviceResult = param.getLocalRunningResult();
			if(null != serviceResult) {
				break;
			}
		}
		//等待超时，查看是否可以阻止事务提交
//		if(null == serviceResult) {
//			boolean isPassed = redisHelper.opsForBlockMarkOperation().isBlockMarkPassed();
//			if(!isPassed) {
//				throw new FatTransactionException(param.getRootTxKey(), "wait result time out , transaction roll back ");
//			}
//			//不能阻止事务提交，进入等待
//			while(true) {
//				//检查是否事务出错
//				redisHelper.opsForServiceError().isServiceError();
//				serviceResult = param.pollFromLocalResultQueue(waitResultBlankTime);
//				if(null != serviceResult) {
//					break;
//				}
//			}
//		}
		return serviceResult;
	}

}

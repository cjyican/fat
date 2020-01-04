package com.cjy.fat.resolve;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.data.TransactionResolveParam;
import com.cjy.fat.resolve.register.ServiceRegister;

@Component
public class CommitResolver {

	@Autowired
	ServiceRegister register;

	/**
	 * 间歇消费时间（毫秒）默认200毫秒
	 * 争抢可提交标识的时候，可能发生错误，避免继续阻塞，导致jdbcConnection/数据库事务一直阻塞，提高响应速度，
	 * 将pop的阻塞时间分段请求
	 */
	@Value("${tx.commit.blankTime:100}")
	private long commitBlankTime ;
	
	/**
	 * 阻塞式提交 , 用于事务
	 * 
	 * @param param
	 */
	public void blockProceed(TransactionResolveParam param) throws Exception{
		
		register.opsForGroupFinishSetOperation().addToGroupFinishSet(param.getLocalTxMark());
		
		if(register.opsForGroupFinishSetOperation().isGroupFinishZSetFull()) {
			
			this.passGroupCancommitList();
			
		}
		
		this.popGroupCanCommitList();
	}
	
	/**
	 * 客户端提交过程
	 * @param param
	 * @throws Exception 
	 */
	public void clientProcced() throws Exception{
		register.opsForGroupFinishSetOperation().addToGroupFinishSet(TransactionContent.getServiceId());
		
		if(register.opsForGroupFinishSetOperation().isGroupFinishZSetFull()) {	
			
			this.passGroupCancommitList();
			
		}
		
	}
	
	/**
	 * 主线程获取服务执行结果
	 * @param param
	 * @return
	 * @throws InterruptedException
	 */
	public Object waitServiceResult(TransactionResolveParam param) throws Exception {
		Object serviceResult = null;
		
		while(true) {
			
			//检查是否事务出错
			if(null != param.getLocalRunningException()) {
				throw param.getLocalRunningException();
			}
			
			serviceResult = param.getLocalRunningResult();
			if(null != serviceResult) {
				break;
			}
			
		}
		
		return serviceResult;
	}
	
	private void passGroupCancommitList() throws Exception{
		
		register.opsForGroupCanCommitListOperation().groupCanCommit();
		
	}
	
	private boolean popGroupCanCommitList() throws Exception{
		while(true) {
			
			boolean canCommit = register.opsForGroupCanCommitListOperation().watchGroupCanCommit(commitBlankTime);
			
			if(!canCommit) {
				
				register.opsForServiceError().isServiceError();
				
				continue;
			}
			
			return true;
			
		}
		
	}

}

package com.cjy.fat.resolve.register;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.exception.FatTransactionException;
import com.cjy.fat.resolve.register.operation.GroupCanCommitListOperation;
import com.cjy.fat.resolve.register.operation.GroupFinishSetOperation;
import com.cjy.fat.resolve.register.operation.GroupServiceSetOperation;
import com.cjy.fat.resolve.register.operation.ServiceErrorOperation;
import com.cjy.fat.resolve.register.servicenode.NameSpace;
import com.cjy.fat.util.StringUtil;

@Component
@ConditionalOnBean(name = {"fatRedis"})
public class RedisRegister extends AbstractRegister{
	
	public static final String REDIS_SUFF = ":";
	
	@Resource(name = "fatRedis")
	RedisTemplate<String, String> redis;
	
	@Value("${spring.application.name}")
	String serviceName;
	
	RedisRegister(){}
	
	@Override
	public String createTxKey(){
		Long txKeyId = redis.opsForValue().increment(getRootNameSpace() + NameSpace.FAT_KEY_ID, 1);
		return  serviceName + StringUtil.initTxKey(txKeyId + "");
	}
	


	@Override
	protected void setRootNameSpace() {
		this.rootNameSpace = NameSpace.FAT_PRE.getNameSpace() + REDIS_SUFF;
	}

	@Override
	protected void initRootNameSpace() throws Exception {
		
	}

	@Override
	protected String appendNameSpace(NameSpace nameSpace) {
		return getRootNameSpace() + TransactionContent.getRootTxKey() + REDIS_SUFF + nameSpace.getNameSpace();
	}
	
	
	/**
	 * 获取某个List的大小
	 * @param txKey
	 * @param keyEnum
	 * @return
	 */
	public long sizeList(String txKey , NameSpace keyEnum){
		return redis.opsForList().size(appendNameSpace(keyEnum));
	}
	
	/**
	 * 获取某个Set的全部元素
	 */
	public Set<String> getAllFromSet(NameSpace setKeyEnum){
		return redis.opsForSet().members(appendNameSpace(setKeyEnum));
	}
	
	/**
	 * 将set的元素写入某个阻塞队列
	 * @param rootTxKey
	 * @param waitMilliesSecond
	 * @return
	 */
	public void pushAllBlockList(NameSpace keyEnum ,Set<String> dataSet) {
		redis.opsForList().leftPushAll(appendNameSpace(keyEnum), dataSet);
	}
	
	/**
	 * 将指定set的元素放入指定的阻塞队列
	 */
	public void pushToBlockListFromSet(NameSpace setTxKeyEnum , NameSpace listTxKeyEnum){
		Set<String> dataSet = getAllFromSet(setTxKeyEnum);
		pushAllBlockList(listTxKeyEnum, dataSet);
	}
	
	/**
	 * 从阻塞队列中获取元素
	 * @param rootTxKey
	 * @param waitMilliesSecond
	 * @return
	 */
	public String popBlockList(String txKey ,NameSpace keyEnum ,long waitMilliesSecond) {
		return redis.opsForList().leftPop(appendNameSpace(keyEnum), waitMilliesSecond, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public ServiceErrorOperation opsForServiceError() {
		if(null == serviceErrorOperation) {
			serviceErrorOperation = new ServiceErrorOperation() {
				
				@Override
				public void serviceError(String serviceName) {
					redis.opsForValue().set(appendNameSpace(NameSpace.SERVICE_ERROR) , ERROR);
				}
				
				@Override
				public void isServiceError() {
					String errorServiceName = redis.opsForValue().get(appendNameSpace(NameSpace.SERVICE_ERROR));
					if(StringUtils.isNotBlank(errorServiceName)){
						FatTransactionException.throwRemoteNodeErrorException(errorServiceName);
					}
				}
				
			};
		}
		return serviceErrorOperation;
	}
	
	@Override
	public GroupCanCommitListOperation opsForGroupCanCommitListOperation() {
		if(null == groupCanCommitListOperation) {
			groupCanCommitListOperation = new GroupCanCommitListOperation() {
				@Override
				public void groupCanCommit() {
					pushToBlockListFromSet(NameSpace.GROUP_SERVICE_SET, NameSpace.GROUP_CANCOMMIT_LIST);
				}
				@Override
				public boolean watchGroupCanCommit( long waitMilliesSecond) {
					String ele = redis.opsForList().leftPop(appendNameSpace(NameSpace.GROUP_CANCOMMIT_LIST), waitMilliesSecond, TimeUnit.MILLISECONDS);
					if(ele != null) {
						return true;
					}
					return false;
				}
			};
		}
		return groupCanCommitListOperation;
	}
	
	@Override
	public GroupFinishSetOperation opsForGroupFinishSetOperation() {
		if(null == groupFinishSetOperation) {
			groupFinishSetOperation = new GroupFinishSetOperation() {
				@Override
				public void addToGroupFinishSet(String localTxKey) throws Exception {
					redis.opsForZSet().add(appendNameSpace(NameSpace.GROUP_FINISH_ZSET), localTxKey ,System.currentTimeMillis());
				}
				@Override
				public long sizeGroupFinishSet() throws Exception {
					return redis.opsForZSet().size(appendNameSpace(NameSpace.GROUP_FINISH_ZSET));
				}
				@Override
				public boolean isGroupFinishZSetFull() throws Exception {
					return sizeGroupFinishSet() == opsForGroupServiceSetOperation().sizeGroupSeviceSet();
				}
			};
		}
		return groupFinishSetOperation;
	}
	
	@Override
	public GroupServiceSetOperation opsForGroupServiceSetOperation() {
		if(null == groupServiceSetOperation) {
			groupServiceSetOperation = new GroupServiceSetOperation() {

				@Override
				public void addToGroupServiceSet(String ele) {
					redis.opsForSet().add(appendNameSpace(NameSpace.GROUP_SERVICE_SET), ele);
				}
				
				@Override
				public long sizeGroupSeviceSet() {
					return redis.opsForSet().size(appendNameSpace(NameSpace.GROUP_SERVICE_SET));
				}

			};
		}
		return groupServiceSetOperation;
	}
	
}

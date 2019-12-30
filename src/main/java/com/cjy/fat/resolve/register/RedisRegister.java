package com.cjy.fat.resolve.register;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.exception.FatTransactionException;
import com.cjy.fat.resolve.register.operation.GroupCanCommitListOperation;
import com.cjy.fat.resolve.register.operation.GroupFinishSetOperation;
import com.cjy.fat.resolve.register.operation.GroupServiceSetOperation;
import com.cjy.fat.resolve.register.operation.MainThreadMarkOperation;
import com.cjy.fat.resolve.register.operation.ServiceErrorOperation;
import com.cjy.fat.resolve.register.servicenode.ServiceNameSpace;
import com.cjy.fat.util.StringUtil;

@Component
public class RedisRegister extends AbstractRegister{
	
	@Resource(name = "fatRedis")
	RedisTemplate<String, String> redis;
	
	@Value("${spring.application.name}")
	String serviceName;
	
	@Override
	public String createTxKey(ServiceNameSpace nameSpace){
		Long txKeyId = redis.opsForValue().increment(nameSpace.getNameSpace(), 1);
		return  serviceName + StringUtil.initTxKey(txKeyId + "");
	}
	
	
	/**
	 * 获取某个List的大小
	 * @param txKey
	 * @param keyEnum
	 * @return
	 */
	public long sizeList(String txKey , ServiceNameSpace keyEnum){
		return redis.opsForList().size(initTxRedisKey(keyEnum));
	}
	
	/**
	 * 获取某个Set的全部元素
	 */
	public Set<String> getAllFromSet(ServiceNameSpace setKeyEnum){
		return redis.opsForSet().members(initTxRedisKey(setKeyEnum));
	}
	
	/**
	 * 将set的元素写入某个阻塞队列
	 * @param rootTxKey
	 * @param waitMilliesSecond
	 * @return
	 */
	public void pushAllBlockList(ServiceNameSpace keyEnum ,Set<String> dataSet) {
		redis.opsForList().leftPushAll(initTxRedisKey(keyEnum), dataSet);
	}
	
	/**
	 * 将指定set的元素放入指定的阻塞队列
	 */
	public void pushToBlockListFromSet(ServiceNameSpace setTxKeyEnum , ServiceNameSpace listTxKeyEnum){
		Set<String> dataSet = getAllFromSet(setTxKeyEnum);
		pushAllBlockList(listTxKeyEnum, dataSet);
	}
	
	/**
	 * 从阻塞队列中获取元素
	 * @param rootTxKey
	 * @param waitMilliesSecond
	 * @return
	 */
	public String popBlockList(String txKey ,ServiceNameSpace keyEnum ,long waitMilliesSecond) {
		return redis.opsForList().leftPop(initTxRedisKey(keyEnum), waitMilliesSecond, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public ServiceErrorOperation opsForServiceError() {
		if(null == serviceErrorOperation) {
			serviceErrorOperation = new ServiceErrorOperation() {
				@Override
				public void serviceNomal() {
					redis.opsForValue().set(initTxRedisKey(ServiceNameSpace.IS_SERVICE_ERROR) , NORMAL);
				}
				@Override
				public void serviceError() {
					redis.opsForValue().set(initTxRedisKey(ServiceNameSpace.IS_SERVICE_ERROR) , ERROR);
				}
				@Override
				public void isServiceError() {
					boolean isError = redis.opsForValue().get(initTxRedisKey(ServiceNameSpace.IS_SERVICE_ERROR)).equals(ERROR);
					if(isError){
						throw new FatTransactionException(TransactionContent.getRootTxKey(), " other service occured error when runnning local transaction");
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
				public void pushGroupServiceSetToGroupCommitList() {
					pushToBlockListFromSet(ServiceNameSpace.GROUP_SERVICE_SET, ServiceNameSpace.GROUP_CANCOMMIT_LIST);
				}
				@Override
				public String popGroupCancommit( long waitMilliesSecond) {
					return redis.opsForList().leftPop(initTxRedisKey(ServiceNameSpace.GROUP_CANCOMMIT_LIST), waitMilliesSecond, TimeUnit.MILLISECONDS);
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
				public void addToGroupFinishSet(String localTxKey) {
					redis.opsForZSet().add(initTxRedisKey(ServiceNameSpace.GROUP_FINISH_ZSET), localTxKey ,System.currentTimeMillis());
				}
				@Override
				public long sizeGroupFinishSet() {
					return redis.opsForZSet().size(initTxRedisKey(ServiceNameSpace.GROUP_FINISH_ZSET));
				}
				@Override
				public boolean isGroupFinishZSetFull() {
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
					redis.opsForSet().add(initTxRedisKey(ServiceNameSpace.GROUP_SERVICE_SET), ele);
				}
				
				@Override
				public long sizeGroupSeviceSet() {
					return redis.opsForSet().size(initTxRedisKey(ServiceNameSpace.GROUP_SERVICE_SET));
				}

			};
		}
		return groupServiceSetOperation;
	}
	
	@Override
	public MainThreadMarkOperation opsForMainThreadMarkOperation() {
		if(null == mainThreadMarkOperation) {
			mainThreadMarkOperation = new MainThreadMarkOperation() {
				
				@Override
				public void setFinshed() {
					redis.opsForValue().set(initTxRedisKey(ServiceNameSpace.MAIN_THREAD_MARK), NORMAL);
				}
				
				@Override
				public boolean isFinshed() {
					if(StringUtils.isNotBlank(redis.opsForValue().get(initTxRedisKey(ServiceNameSpace.MAIN_THREAD_MARK)))) {
						return true;
					}
					return false;
				}
			};
		}
		return mainThreadMarkOperation;
	}
	
}

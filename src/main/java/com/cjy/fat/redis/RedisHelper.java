package com.cjy.fat.redis;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.exception.FatTransactionException;
import com.cjy.fat.redis.constant.RedisKeyEnum;
import com.cjy.fat.redis.operation.BlockMarkOperation;
import com.cjy.fat.redis.operation.GroupCanCommitListOperation;
import com.cjy.fat.redis.operation.GroupFinishSetOperation;
import com.cjy.fat.redis.operation.GroupServiceSetOperation;
import com.cjy.fat.redis.operation.ServiceErrorOperation;
import com.cjy.fat.redis.operation.ServiceSetOperation;
import com.cjy.fat.util.CollectionUtil;
import com.cjy.fat.util.StringUtil;

@Component
public class RedisHelper {
	
	@Resource(name = "fatRedis")
	RedisTemplate<String, String> redis;
	
	private static final String NORMAL = "0";
	
	private static final String ERROR = "1";
	
	/**
	 * 将n个serviceId加入到所属的txKey中，后面的服务从这里获取作为服务标识
	 * @param txKey
	 * @param serviceId
	 */
	public void addToTxServiceIdSet(String txKey ,String[] array){
		redis.opsForSet().add(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_ID_SET, txKey), array);
	}
	
	/**
	 * 从service_id set中弹出一个元素
	 * @return
	 */
	public String popFromServiceIdSet(String txKey){
		return redis.opsForSet().pop(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_ID_SET, txKey));
	}
	
	
	/**
	 * 生成TxKey
	 * @param serviceName
	 * @return
	 */
	public String createTxKey(String serviceName){
		String redisTxKeyId = redis.opsForValue().get(RedisKeyEnum.FAT_KEY_ID.getRedisKey());
		if(StringUtils.isBlank(redisTxKeyId)){
			//初始化
			redis.opsForValue().set(RedisKeyEnum.FAT_KEY_ID.getRedisKey(), String.valueOf(System.currentTimeMillis()));
		}
		Long txKeyId = redis.opsForValue().increment(RedisKeyEnum.FAT_KEY_ID.getRedisKey(), 1);
		return StringUtil.appendStr(serviceName ,"-" ,String.valueOf(txKeyId) );
	}
	
	public static String initTxRedisKey(RedisKeyEnum txRedisKeyEnum , String txKey){
		return StringUtil.appendStr(RedisKeyEnum.FAT_PRE.getRedisKey() , txKey , txRedisKeyEnum.getRedisKey() );
	}
	
	/**
	 * 获取某个List的大小
	 * @param txKey
	 * @param keyEnum
	 * @return
	 */
	public long sizeList(String txKey , RedisKeyEnum keyEnum){
		return redis.opsForList().size(RedisHelper.initTxRedisKey(keyEnum, txKey));
	}
	
	/**
	 * 获取某个Set的全部元素
	 */
	public Set<String> getAllFromSet(String txKey, RedisKeyEnum setKeyEnum){
		return redis.opsForSet().members(RedisHelper.initTxRedisKey(setKeyEnum, txKey));
	}
	
	/**
	 * 将set的元素写入某个阻塞队列
	 * @param rootTxKey
	 * @param waitMilliesSecond
	 * @return
	 */
	public void pushAllBlockList(String txKey ,RedisKeyEnum keyEnum ,Set<String> dataSet) {
		redis.opsForList().leftPushAll(RedisHelper.initTxRedisKey(keyEnum, txKey), dataSet);
	}
	
	/**
	 * 将指定set的元素放入指定的阻塞队列
	 */
	public void pushToBlockListFromSet(String setKey ,RedisKeyEnum setTxKeyEnum ,String listKey , RedisKeyEnum listTxKeyEnum){
		Set<String> dataSet = getAllFromSet(setKey , setTxKeyEnum);
		CollectionUtil.checkDataSet(dataSet, setKey);
		pushAllBlockList(listKey, listTxKeyEnum, dataSet);
	}
	
	
	/**
	 * 将指定set的元素放入另一个set
	 */
	public void pushToSetFromSet(String fromSetKey ,RedisKeyEnum fromSetTxKeyEnum ,String toSetKey , RedisKeyEnum toSetTxKeyEnum){
		Set<String> dataSet = getAllFromSet(fromSetKey , fromSetTxKeyEnum);
		CollectionUtil.checkDataSet(dataSet, fromSetKey);
		redis.opsForSet().add(RedisHelper.initTxRedisKey(toSetTxKeyEnum, toSetKey), dataSet.toArray(new String[dataSet.size()]));
	}
	
	/**
	 * 从阻塞队列中获取元素
	 * @param rootTxKey
	 * @param waitMilliesSecond
	 * @return
	 */
	public String popBlockList(String txKey ,RedisKeyEnum keyEnum ,long waitMilliesSecond) {
		return redis.opsForList().leftPop(RedisHelper.initTxRedisKey(keyEnum, txKey), waitMilliesSecond, TimeUnit.MILLISECONDS);
	}
	
	private ServiceErrorOperation serviceErrorOperation;
	
	public ServiceErrorOperation opsForServiceError() {
		if(null == serviceErrorOperation) {
			serviceErrorOperation = new ServiceErrorOperation() {
				@Override
				public void serviceNomal(String txKey) {
					redis.opsForValue().set(RedisHelper.initTxRedisKey(RedisKeyEnum.IS_SERVICE_ERROR, txKey) , NORMAL);
				}
				@Override
				public void serviceError(String txKey) {
					redis.opsForValue().set(RedisHelper.initTxRedisKey(RedisKeyEnum.IS_SERVICE_ERROR, txKey) , ERROR);
				}
				@Override
				public void isServiceError(String txKey) {
					boolean isError = redis.opsForValue().get(RedisHelper.initTxRedisKey(RedisKeyEnum.IS_SERVICE_ERROR, txKey)).equals(ERROR);
					if(isError){
						throw new FatTransactionException(txKey , txKey + " other service occured error when runnning local transaction");
					}
				}
			};
		}
		return serviceErrorOperation;
	}
	
	private ServiceSetOperation serviceSetOperation;
	
	public ServiceSetOperation opsForServiceSetOperation() {
		if(null == serviceSetOperation) {
			serviceSetOperation = new ServiceSetOperation() {
				@Override
				public long sizeServiceSet(String txKey){
					return redis.opsForSet().size(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_SET, txKey));
				}
				@Override
				public void addToServiceSet(String txKey , String serviceName){
					redis.opsForSet().add(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_SET, txKey), serviceName);
				}
				@Override
				public void addToServiceSet(String txKey, String[] serviceNameArray) {
					redis.opsForSet().add(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_SET, txKey), serviceNameArray);
				}
			};
		}
		return serviceSetOperation;
	}
	
	private GroupCanCommitListOperation groupCanCommitListOperation;
	
	public GroupCanCommitListOperation opsForGroupCanCommitListOperation() {
		if(null == groupCanCommitListOperation) {
			groupCanCommitListOperation = new GroupCanCommitListOperation() {
				@Override
				public void pushGroupServiceSetToGroupCommitList(String txKey) {
					pushToBlockListFromSet(txKey, RedisKeyEnum.GROUP_SERVICE_SET, txKey, RedisKeyEnum.GROUP_CANCOMMIT_LIST);
				}
				@Override
				public String popGroupCancommit(String rootTxKey, long waitMilliesSecond) {
					return redis.opsForList().leftPop(RedisHelper.initTxRedisKey(RedisKeyEnum.GROUP_CANCOMMIT_LIST, rootTxKey), waitMilliesSecond, TimeUnit.MILLISECONDS);
				}
			};
		}
		return groupCanCommitListOperation;
	}
	
	private GroupFinishSetOperation groupFinishSetOperation;
	
	public GroupFinishSetOperation opsForGroupFinishSetOperation() {
		if(null == groupFinishSetOperation) {
			groupFinishSetOperation = new GroupFinishSetOperation() {
				@Override
				public void addGroupFinishSet(String rootTxKey, String localTxKey) {
					redis.opsForZSet().add(RedisHelper.initTxRedisKey(RedisKeyEnum.GROUP_FINISH_ZSET , rootTxKey), localTxKey ,System.currentTimeMillis());
				}
				@Override
				public long sizeGroupFinishSet(String rootTxKey) {
					return redis.opsForZSet().size(RedisHelper.initTxRedisKey(RedisKeyEnum.GROUP_FINISH_ZSET , rootTxKey));
				}
				@Override
				public boolean isGroupFinishZSetFull(String rootTxKey) {
					return sizeGroupFinishSet(rootTxKey) == opsForGroupServiceSetOperation().sizeGroupSeviceSet();
				}
			};
		}
		return groupFinishSetOperation;
	}
	
	private GroupServiceSetOperation groupServiceSetOperation ; 
	
	public GroupServiceSetOperation opsForGroupServiceSetOperation() {
		if(null == groupServiceSetOperation) {
			groupServiceSetOperation = new GroupServiceSetOperation() {

				@Override
				public void addToGroupServiceSet(String rootTxKey, String serviceId) {
					redis.opsForSet().add(RedisHelper.initTxRedisKey(RedisKeyEnum.GROUP_SERVICE_SET, rootTxKey), serviceId);
				}
				
				@Override
				public long sizeGroupSeviceSet() {
					return redis.opsForSet().size(RedisHelper.initTxRedisKey(RedisKeyEnum.GROUP_SERVICE_SET, TransactionContent.getRootTxKey()));
				}

				@Override
				public void addToGroupServiceSet(String rootTxKey, String... serviceIds) {
					redis.opsForSet().add(RedisHelper.initTxRedisKey(RedisKeyEnum.GROUP_SERVICE_SET, rootTxKey), serviceIds);
				}
			};
		}
		return groupServiceSetOperation;
	}
	
	private BlockMarkOperation blockMarkOperation;
	
	public BlockMarkOperation opsForBlockMarkOperation() {
		if(null == blockMarkOperation) {
			blockMarkOperation = new BlockMarkOperation() {
				@Override
				public void passBlockMark(String txKey , RedisKeyEnum markEnum) {
					redis.opsForValue().set(RedisHelper.initTxRedisKey(markEnum, txKey), NORMAL);
				}
				@Override
				public boolean isBlockMarkPassed(String txKey , RedisKeyEnum markEnum) {
					String isPassed = redis.opsForValue().get(RedisHelper.initTxRedisKey(markEnum, txKey));
					if(StringUtils.isNotBlank(isPassed)) {
						return true;
					}
					return false;
				}
				@Override
				public void unPassBlockMark(String txKey, RedisKeyEnum markEnum) {
					redis.delete(RedisHelper.initTxRedisKey(markEnum, txKey));
				}
			};
		}
		return blockMarkOperation;
	}
	  
}

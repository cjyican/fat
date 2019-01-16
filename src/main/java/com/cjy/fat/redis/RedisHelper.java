package com.cjy.fat.redis;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.cjy.fat.exception.FatTransactionException;
import com.cjy.fat.redis.constant.RedisKeyEnum;
import com.cjy.fat.util.CollectionUtil;
import com.cjy.fat.util.StringUtil;

@Component
public class RedisHelper {
	
	@Resource(name = "fatRedis")
	RedisTemplate<String, String> redis;
	
	private static final String NORMAL = "0";
	
	private static final String ERROR = "1";
	
	private static final String SERVICE = "-service-";
	
	/**
	 * 将n个serviceId加入到所属的txKey中，后面的服务从这里获取作为服务标识
	 * @param txKey
	 * @param serviceId
	 */
	public void addToTxServiceSet(String txKey ,int serviceCount){
		String[] array = new String[serviceCount];
		for(int i = 0 ; i < serviceCount ; i ++){
			array[i] = txKey + SERVICE + i;
		}
		redis.opsForSet().add(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_ID_SET, txKey), array);
		this.opsForServiceSetOperation().addToServiceSet(txKey, array);
		
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
	 * 写入阻塞队列
	 * @param rootTxKey
	 * @param waitMilliesSecond
	 * @return
	 */
	public void pushBlockList(String txKey ,RedisKeyEnum keyEnum ,String content) {
		redis.opsForList().leftPush(RedisHelper.initTxRedisKey(keyEnum, txKey), content);
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
	
	//事务吃错标识操作接口
	private ServiceErrorOperation serviceErrorOperation;
	
	public ServiceErrorOperation opsForServiceError() {
		if(null == serviceErrorOperation) {
			serviceErrorOperation = new ServiceErrorOperation() {
				@Override
				public void txServiceNomal(String txKey) {
					redis.opsForValue().set(RedisHelper.initTxRedisKey(RedisKeyEnum.IS_SERVICE_ERROR, txKey) , NORMAL);
				}
				@Override
				public void txServiceError(String txKey) {
					redis.opsForValue().set(RedisHelper.initTxRedisKey(RedisKeyEnum.IS_SERVICE_ERROR, txKey) , ERROR);
				}
				@Override
				public void isTxServiceError(String txKey) {
					boolean isError = redis.opsForValue().get(RedisHelper.initTxRedisKey(RedisKeyEnum.IS_SERVICE_ERROR, txKey)).equals(ERROR);
					if(isError){
						throw new FatTransactionException(txKey , txKey+" occured other transaction error when runnning local transaction");
					}
				}
			};
		}
		return serviceErrorOperation;
	}
	
	private CanCommitListOperation canCommitListOperation;
	
	public CanCommitListOperation opsForCanCommitListOperation() {
		if(null == canCommitListOperation) {
			canCommitListOperation = new CanCommitListOperation() {
				@Override
				public long sizeCancommitList(String txKey){
					return sizeList(txKey, RedisKeyEnum.SERVICE_CANCOMMIT_LIST);
				}

				@Override
				public void pushToCancommitListFromZSet(String txKey , long size){
					Set<String> dataSet = redis.opsForZSet().range(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_CANCOMMIT_ZSET, txKey), 0, size);
					if(dataSet != null && dataSet.size() > 0){
						redis.opsForList().leftPushAll(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_CANCOMMIT_LIST, txKey), dataSet);
					}
				}
			};
		}
		return canCommitListOperation;
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
	
	private ServiceReadyCommitListOperation serviceReadyCommitListOperation;
	
	public ServiceReadyCommitListOperation opsForServiceReadyCommitListOperation() {
		if(null == serviceReadyCommitListOperation) {
			serviceReadyCommitListOperation = new ServiceReadyCommitListOperation() {
				@Override
				public void pushServiceSetToReadCommitList(String txKey) {
					pushToBlockListFromSet(txKey, RedisKeyEnum.SERVICE_SET, txKey, RedisKeyEnum.SERVICE_READYCOMMIT_LIST);
				}
				@Override
				public void pushReadyCommitList(String txKey, String serviceName) {
					redis.opsForList().leftPush(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_READYCOMMIT_LIST, txKey), serviceName);
				}
				@Override
				public String popReadyCommitList(String txKey, long waitMilliesSecond) {
					return redis.opsForList().leftPop(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_READYCOMMIT_LIST,txKey), waitMilliesSecond, TimeUnit.MILLISECONDS);
				}
			};
		}
		return serviceReadyCommitListOperation;
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
				@Override
				public void pushServiceSetToGroupCommitList(String txKey, String rootTxKey) {
					pushToBlockListFromSet(txKey, RedisKeyEnum.SERVICE_SET, rootTxKey, RedisKeyEnum.GROUP_CANCOMMIT_LIST);
				}
			};
		}
		return groupCanCommitListOperation;
	}
	
	private ServiceFinishTimeZsetOperation serviceFinishTimeZetOpeation;
	
	public ServiceFinishTimeZsetOperation opsForServiceFinishTimeZsetOperation() {
		if(null == serviceFinishTimeZetOpeation) {
			serviceFinishTimeZetOpeation = new ServiceFinishTimeZsetOperation() {
				@Override
				public void addServiceFinishZSet(String txKey, String serviceName) {
					redis.opsForZSet().add(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_READYCOMMIT_ZSET , txKey), serviceName,
							System.currentTimeMillis());
				}
				@Override
				public long sizeServiceFinishZSet(String txKey) {
					return redis.opsForZSet().size(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_READYCOMMIT_ZSET, txKey));
				}
				@Override
				public boolean isServiceFinishZSetFull(String txKey) {
					return sizeServiceFinishZSet(txKey) == opsForServiceSetOperation().sizeServiceSet(txKey);
				}
			};
		}
		return serviceFinishTimeZetOpeation;
	}
	
	private GroupKeySetOperation groupKeySetOperation;
	
	public GroupKeySetOperation opsForGroupKeySetOperation() {
		if(null == groupKeySetOperation) {
			groupKeySetOperation = new GroupKeySetOperation() {
				@Override
				public void addToGroupKeySet(String rootTxKey , String localTxKey) {
					redis.opsForSet().add(RedisHelper.initTxRedisKey(RedisKeyEnum.GROUP_KEY_SET, rootTxKey) , localTxKey);
				}
				@Override
				public long sizeGroupKeySet(String rootTxKey) {
					return redis.opsForSet().size(RedisHelper.initTxRedisKey(RedisKeyEnum.GROUP_KEY_SET, rootTxKey));
				}
			};
		}
		return groupKeySetOperation;
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
					return sizeGroupFinishSet(rootTxKey) == opsForGroupKeySetOperation().sizeGroupKeySet(rootTxKey);
				}
			};
		}
		return groupFinishSetOperation;
	}
	
	private ServiceCanCommitZSetOperation serviceCanCommitZSetOperation;
	
	public ServiceCanCommitZSetOperation opsForServiceCanCommitZSetOperation() {
		if(null == serviceCanCommitZSetOperation) {
			serviceCanCommitZSetOperation = new ServiceCanCommitZSetOperation() {
				@Override
				public void addToCancommitZSet(String txKey, String content) {
					redis.opsForZSet().add(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_CANCOMMIT_ZSET, txKey), content , System.currentTimeMillis());
				}
				@Override
				public long sizeCancommitZSet(String txKey) {
					return redis.opsForZSet().size(RedisHelper.initTxRedisKey(RedisKeyEnum.SERVICE_CANCOMMIT_ZSET, txKey));
				}
				@Override
				public boolean isCancommitZSetFull(String txKey) {
					return sizeCancommitZSet(txKey) == opsForServiceSetOperation().sizeServiceSet(txKey);
				}
			};
		}
		return serviceCanCommitZSetOperation;
	}
	
	private GroupServiceSetOperation groupServiceSetOperation ; 
	
	public GroupServiceSetOperation opsForGroupServiceSetOperation() {
		if(null == groupServiceSetOperation) {
			groupServiceSetOperation = new GroupServiceSetOperation() {
				@Override
				public void addLocalServiceSetToGroupServiceSet(String localTxKey, String rootKey) {
					pushToSetFromSet(localTxKey, RedisKeyEnum.SERVICE_SET, rootKey, RedisKeyEnum.GROUP_SERVICE_SET);
				}

				@Override
				public void addToGroupServiceSet(String rootTxKey, String serviceId) {
					redis.opsForSet().add(RedisHelper.initTxRedisKey(RedisKeyEnum.GROUP_SERVICE_SET, rootTxKey), serviceId);
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

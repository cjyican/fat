package com.cjy.fat.resolve.register;

import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.resolve.register.operation.GroupCanCommitListOperation;
import com.cjy.fat.resolve.register.operation.GroupFinishSetOperation;
import com.cjy.fat.resolve.register.operation.GroupServiceSetOperation;
import com.cjy.fat.resolve.register.operation.MainThreadMarkOperation;
import com.cjy.fat.resolve.register.operation.ServiceErrorOperation;
import com.cjy.fat.resolve.register.servicenode.ServiceNameSpace;
import com.cjy.fat.util.StringUtil;

public interface ServiceRegister {
	
	static final String NORMAL = "0";
	
	static final String ERROR = "1";
	
	default String initTxRedisKey(ServiceNameSpace nameSapce){
		return StringUtil.appendStr(ServiceNameSpace.FAT_PRE.getNameSpace() , TransactionContent.getRootTxKey() , nameSapce.getNameSpace() );
	}
	
	/**
	 * 生成一次分布式事务的key
	 * @param serviceName 当前服务系统名称
	 * @return
	 */
	String createTxKey(String serviceName);
	
	/**
	 * 获取事务失败操作类
	 * @return
	 */
	ServiceErrorOperation opsForServiceError();

	/**
	 * 获取事务组操作类
	 * @return
	 */
	GroupCanCommitListOperation opsForGroupCanCommitListOperation();

	/**
	 * 获取事务组完成的服务操作类
	 * @return
	 */
	GroupFinishSetOperation opsForGroupFinishSetOperation();

	/**
	 * 获取事务组服务元素操作类
	 * @return
	 */
	GroupServiceSetOperation opsForGroupServiceSetOperation();

	/**
	 * 获取主流程完成表示操作类
	 * @return
	 */
	MainThreadMarkOperation opsForMainThreadMarkOperation();
	
}

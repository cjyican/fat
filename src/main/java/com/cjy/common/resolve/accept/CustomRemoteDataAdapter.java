package com.cjy.common.resolve.accept;

import java.util.Map;

/**
    * 自定义服务api需要提供该接口的实现已获取远程信息
 * @author cjy
 *
 */
public interface CustomRemoteDataAdapter {
	
	/**
	 * eg:dubbo:RpcContent.getAttachments();
	 * @return
	 */
	Map<String , String> convertRemoteDataToMap();
	
}

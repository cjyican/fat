package com.cjy.fat.resolve;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.resolve.accept.RemoteTransactionAccepter;

@Component
public class RemoteTransactionDataResolver {
	
	@Autowired
	List<RemoteTransactionAccepter> accepters;
	
	/**
	 * 初始化远程事务信息
	 */
	public void init() {
		//初始化当前容器
		TransactionContent.initContainer();
		
		if(accepters != null && !accepters.isEmpty()) {
			for(RemoteTransactionAccepter accepter : accepters) {
				accepter.acceptRemoteTransactionData();
			}
		}
		
	}
	
}

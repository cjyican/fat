package com.cjy.fat.resolve.register;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.cjy.fat.config.ZooTemplate;
import com.cjy.fat.exception.FatTransactionException;
import com.cjy.fat.resolve.register.operation.GroupCanCommitListOperation;
import com.cjy.fat.resolve.register.operation.GroupFinishSetOperation;
import com.cjy.fat.resolve.register.operation.GroupServiceSetOperation;
import com.cjy.fat.resolve.register.operation.ServiceErrorOperation;
import com.cjy.fat.resolve.register.servicenode.NameSpace;
import com.cjy.fat.resolve.register.servicenode.ZookeeperNameSpaceAppender;

@Component
@ConditionalOnBean(name= {"zooTemplate"})
@Primary
public class ZookeeperRegister extends AbstractRegister{
	
	@Autowired
	ZooTemplate zooTemplate;
	
	ZookeeperRegister(){
		super(new ZookeeperNameSpaceAppender());
	}
	
	@Override
	public String createTxKey(NameSpace nameSpace) throws Exception{
		return zooTemplate.createSeqNode(nameSpace);
	}

	@Override
	public ServiceErrorOperation opsForServiceError() {
		if(null == serviceErrorOperation) {
			serviceErrorOperation = new ServiceErrorOperation() {
				
				@Override
				public void serviceNomal() throws Exception {
					zooTemplate.setData(appendNameSpace(NameSpace.FAT_PRE), NORMAL);
				}
				
				@Override
				public void serviceError() throws Exception {
					zooTemplate.setData(appendNameSpace(NameSpace.FAT_PRE), ERROR);
				}
				
				@Override
				public void isServiceError() throws Exception {
					String serviceError = zooTemplate.getData(appendNameSpace(NameSpace.IS_SERVICE_ERROR));
					if(serviceError.equals(ERROR)) {
						throw new FatTransactionException(" other service occured error when runnning local transaction");
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
				public void groupCanCommit() throws Exception {
					zooTemplate.creteNode(appendNameSpace(NameSpace.GROUP_CANCOMMIT_LIST));
				}

				@Override
				public String watchGroupCanCommit(long waitMilliesSecond) throws Exception {
					CountDownLatch latch = new CountDownLatch(1);
					
					String path = zooTemplate.exists(appendNameSpace(NameSpace.GROUP_CANCOMMIT_LIST) , new TxWatcher() {

						@Override
						protected void watch(WatchedEvent event) {
							latch.countDown();
						}
						
					});
					
					if(path != null) {
						return path;
					}
					
					latch.wait();
					
					return appendNameSpace(NameSpace.GROUP_CANCOMMIT_LIST);
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
				public long sizeGroupFinishSet() throws Exception {
					List<String> childrens = zooTemplate.getChildrens(appendNameSpace(NameSpace.GROUP_FINISH_ZSET));
					if(childrens != null) {
						return childrens.size();
					}
					return 0;
				}
				
				@Override
				public boolean isGroupFinishZSetFull() throws Exception {
					return sizeGroupFinishSet() == opsForGroupServiceSetOperation().sizeGroupSeviceSet();
				}
				
				@Override
				public void addToGroupFinishSet(String ele) throws Exception{
					zooTemplate.createChildren(appendNameSpace(NameSpace.GROUP_FINISH_ZSET), ele);
					
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
				public long sizeGroupSeviceSet() throws Exception {
					List<String> childrens = zooTemplate.getChildrens(appendNameSpace(NameSpace.GROUP_SERVICE_SET));
					if(childrens != null) {
						return childrens.size();
					}
					return 0;
				}
				
				@Override
				public void addToGroupServiceSet(String ele) throws Exception {
					zooTemplate.createChildren(appendNameSpace(NameSpace.GROUP_SERVICE_SET), ele);
				}
			};
		}
		return groupServiceSetOperation;
	}

}

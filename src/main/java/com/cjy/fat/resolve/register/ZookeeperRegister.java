package com.cjy.fat.resolve.register;

import java.util.List;
import java.util.concurrent.Semaphore;

import javax.annotation.PostConstruct;

import org.apache.zookeeper.WatchedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.cjy.fat.config.ZooTemplate;
import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.exception.FatTransactionException;
import com.cjy.fat.resolve.register.operation.GroupCanCommitListOperation;
import com.cjy.fat.resolve.register.operation.GroupFinishSetOperation;
import com.cjy.fat.resolve.register.operation.GroupServiceSetOperation;
import com.cjy.fat.resolve.register.operation.ServiceErrorOperation;
import com.cjy.fat.resolve.register.operation.zookeeper.TxWatcher;
import com.cjy.fat.resolve.register.servicenode.NameSpace;

@Component
@ConditionalOnBean(name= {"zooTemplate"})
@Primary
public class ZookeeperRegister extends AbstractRegister{
	
	public static final String ZOO_PRE = "/";
	
	@Autowired
	ZooTemplate zooTemplate;
	
	ZookeeperRegister(){};

	@PostConstruct
	@Override
	protected void initRootNameSpace()  throws Exception{
		setRootNameSpace();
		String rootNameSpace = getRootNameSpace();
		
		boolean exist = zooTemplate.exists(rootNameSpace);
		if(!exist) {
			zooTemplate.createNode(rootNameSpace, System.currentTimeMillis() + "");
		}
		
	}
	
	@Override
	protected void setRootNameSpace() {
		this.rootNameSpace = ZOO_PRE + NameSpace.FAT_PRE.getNameSpace();
	}
	
	@Override
	protected String appendNameSpace(NameSpace nameSpace) {
		return TransactionContent.getRootTxKey() + ZOO_PRE + nameSpace.getNameSpace();
	}
	
	@Override
	public String createTxKey() throws Exception{
		return zooTemplate.createSeqNode(getRootNameSpace() + ZOO_PRE + NameSpace.FAT_KEY_ID.getNameSpace() , System.currentTimeMillis() + "");
	}

	@Override
	public ServiceErrorOperation opsForServiceError() {
		if(null == serviceErrorOperation) {
			serviceErrorOperation = new ServiceErrorOperation() {
				
				@Override
				public void serviceError(String serviceName) throws Exception {
					try {
						zooTemplate.createNode(appendNameSpace(NameSpace.SERVICE_ERROR) , serviceName);
					} catch (Exception e) {
						// TODO
					}
					
				}
				
				@Override
				public void isServiceError() throws Exception {
					String errorNodePath = appendNameSpace(NameSpace.SERVICE_ERROR);
					boolean isError = zooTemplate.exists(errorNodePath);
					if(isError) {
						String errorServiceName = zooTemplate.getData(errorNodePath);
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
				public void groupCanCommit() throws Exception {
					zooTemplate.createNode(appendNameSpace(NameSpace.GROUP_CANCOMMIT_LIST) , System.currentTimeMillis() + "");
				}

				@Override
				public boolean watchGroupCanCommit(long waitMilliesSecond) throws Exception {
					Semaphore sem = new Semaphore(1, true);
					sem.acquire();
					//监听可提交节点
					zooTemplate.exists(appendNameSpace(NameSpace.GROUP_CANCOMMIT_LIST) , new TxWatcher() {

						@Override
						protected void watch(WatchedEvent event) {
							sem.release();
						}
						
					});
					
					//监听出错节点
					zooTemplate.exists(appendNameSpace(NameSpace.SERVICE_ERROR), new TxWatcher() {
						
						@Override
						protected void watch(WatchedEvent event) throws Exception {
							
							String errorServiceName = zooTemplate.getData(appendNameSpace(NameSpace.SERVICE_ERROR));
							TransactionContent.getTxParam().setLocalRunningException(FatTransactionException.buildRemoteNodeErrorException(errorServiceName));
							
							sem.release();
						}
					});
					
					sem.acquire();
					
					if(TransactionContent.getTxParam().getLocalRunningException() != null) {
						throw TransactionContent.getTxParam().getLocalRunningException();
					}
					
					return true;
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
					zooTemplate.createChildren(appendNameSpace(NameSpace.GROUP_FINISH_ZSET), ele , System.currentTimeMillis() +"");
					
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
					zooTemplate.createChildren(appendNameSpace(NameSpace.GROUP_SERVICE_SET), ele,System.currentTimeMillis()+ "");
				}
			};
		}
		return groupServiceSetOperation;
	}


}

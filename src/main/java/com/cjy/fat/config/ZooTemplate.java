package com.cjy.fat.config;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cjy.fat.resolve.register.ZookeeperRegister;
import com.cjy.fat.resolve.register.operation.zookeeper.TxWatcher;

public class ZooTemplate {
	
	private static Logger LOG = LoggerFactory.getLogger(ZooTemplate.class);
	
	private static final String defaultCharset = "UTF-8";
	
	private static ZooKeeper zoo ;
	
	public ZooTemplate () {
		
	}
	
	public ZooTemplate (String host , int sessionTimeout) throws Exception{
		Semaphore sem = new Semaphore(1,true);
		sem.acquire();
		ZooKeeper zoo = new ZooKeeper(host, sessionTimeout, new Watcher() {
			
			@Override
			public void process(WatchedEvent event) {
				
				sem.release();
				
			}
			
		});
		
		sem.tryAcquire(sessionTimeout, TimeUnit.MILLISECONDS);
		
		States connectionStats = zoo.getState();
		if(!connectionStats.isConnected()) {
			
			zoo.close();
			
			throw new RuntimeException("connect zookeeper server host:" + host + " timeout ");
		}
		
		ZooTemplate.zoo = zoo;
	}
	
	public String createNode(String path) throws Exception {
		return zoo.create(path, null,  ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
	}
	
	public String createNode(String path , String data) throws Exception {
		return zoo.create(path, data.getBytes(defaultCharset),  ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
	}
	
	public String createSeqNode(String path, String data) throws Exception {
		return zoo.create(path, data.getBytes(defaultCharset), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
	}

	public String getData(String path) throws Exception {
		byte[] data = zoo.getData(path, null, null);
		if(null == data) {
			return null;
		}
		return new String(data, defaultCharset);
	}
	
	public String getData(String path , TxWatcher watcher) throws Exception {
		byte[] data = zoo.getData(path, watcher, null);
		if(null == data) {
			return null;
		}
		return new String(data, defaultCharset);
	}
	
	public void setData(String path , String data) throws Exception {
		zoo.setData(path, data.getBytes(defaultCharset),zoo.exists(path, false).getVersion());
	}

	public void createChildren(String parentPath, String childrenPath , String data) throws Exception {
		Stat stat = zoo.exists(parentPath, false);
		if(stat == null) {
			try {
				zoo.create(parentPath, parentPath.getBytes(defaultCharset), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			} catch (NodeExistsException e) { // 这里可能会出现并发问题，上面exist都返回了false，这里会同时尝试创建父节点，这里直接catch，不使用同步处理，因为目的只是创建父节点，使其存在，并不时对已经存在的父节点进行更新或者其他处理
				LOG.info("Node:" + parentPath + " exist");
			}
		}
		zoo.create(parentPath + ZookeeperRegister.ZOO_PRE  + childrenPath, data.getBytes(defaultCharset), ZooDefs.Ids.OPEN_ACL_UNSAFE,  CreateMode.PERSISTENT);
	}

	public List<String> getChildrens(String parenPath) throws Exception{
		return zoo.getChildren(parenPath, false);
	}

	public boolean exists(String path, TxWatcher txWatcher) throws Exception {
		Stat stat = zoo.exists(path, txWatcher);
		if(stat != null) {
			return true;
		}
		return false;
	}
	
	public boolean exists(String path) throws Exception {
		Stat stat = zoo.exists(path, null);
		if(stat != null) {
			return true;
		}
		return false;
	}
	
	
}

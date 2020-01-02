package com.cjy.fat.config;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.data.Stat;

import com.cjy.fat.resolve.register.TxWatcher;
import com.cjy.fat.resolve.register.ZookeeperRegister;

public class ZooTemplate {
	
	private static final String defaultCharset = "UTF-8";
	
	private static ZooKeeper zoo ;
	
	public ZooTemplate () {
		
	}
	
	public ZooTemplate (String host , int sessionTimeout) throws Exception{
		CountDownLatch connectLatch = new CountDownLatch(1);
		
		ZooKeeper zoo = new ZooKeeper(host, sessionTimeout, new Watcher() {
			
			@Override
			public void process(WatchedEvent event) {
				
				connectLatch.countDown();
				
			}
			
		});
		
		connectLatch.await(sessionTimeout, TimeUnit.MILLISECONDS);
		
		States connectionStats = zoo.getState();
		if(!connectionStats.isConnected()) {
			
			zoo.close();
			
			throw new RuntimeException("connect zookeeper server host:" + host + " timeout ");
		}
		
		ZooTemplate.zoo = zoo;
	}
	
	public String creteNode(String path) throws Exception {
		return zoo.create(path, null,  ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
	}
	
	public String creteNode(String path , String data) throws Exception {
		return zoo.create(path, data.getBytes(),  ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
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
	
	public void setData(String path , String data) throws Exception {
		zoo.setData(path, data.getBytes(defaultCharset), 0);
	}

	public void createChildren(String parentPath, String childrenPath , String data) throws Exception {
		Stat stat = zoo.exists(parentPath, false);		
		if(stat == null) {
			zoo.create(parentPath, null, null, CreateMode.PERSISTENT);
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

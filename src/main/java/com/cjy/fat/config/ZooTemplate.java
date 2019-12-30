package com.cjy.fat.config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;

import com.cjy.fat.resolve.register.servicenode.ServiceNameSpace;

public class ZooTemplate {
	
	private static ZooKeeper zoo ;
	
	private ZooTemplate () {
		
	}
	
	public static void initZooTemplate(String host , int sessionTimeout) throws Exception{
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
	
	public static String createSeqNode(ServiceNameSpace nameSpace) throws Exception {
		return zoo.create(nameSpace.getNameSpace(), null, null, CreateMode.PERSISTENT_SEQUENTIAL);
	}

	public static String getNodeData() {
		// TODO Auto-generated method stub
		
	}
	
	
	
}

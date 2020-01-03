package com.cjy.fat.resolve.register.operation;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import com.cjy.fat.data.TransactionContent;
import com.cjy.fat.data.TransactionThreadLocal;

public abstract class TxWatcher implements Watcher{
	
	private TransactionThreadLocal local;
	
	public TxWatcher(){
		local = TransactionContent.getLocalData();
	}

	@Override
	public void process(WatchedEvent event) {
		TransactionContent.setContainer(local);
		this.watch(event);
	}
	
	protected abstract void watch(WatchedEvent event);

}

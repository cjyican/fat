package com.cjy.common.exception;

public class TxException extends RuntimeException{

	private static final long serialVersionUID = 6753994776962949879L;
	

	private String txKey ; 
	
	public TxException() {
        super();
    }
	
	public TxException(String message) {
	    super(message);
	}

	public TxException(String txKey  , String message ) {
		super(message);
		this.txKey = txKey;
	}

	public String getTxKey() {
		return txKey;
	}

	public void setTxKey(String txKey) {
		this.txKey = txKey;
	}
	
	
}

package com.cjy.fat.exception;

public class FatTransactionException extends RuntimeException{

	private static final long serialVersionUID = 6753994776962949879L;
	

	private String txKey ; 
	
	public FatTransactionException() {
        super();
    }
	
	public FatTransactionException(String message) {
	    super(message);
	}

	public FatTransactionException(String txKey  , String message ) {
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

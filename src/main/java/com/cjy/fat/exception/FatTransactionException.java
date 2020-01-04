package com.cjy.fat.exception;

public class FatTransactionException extends RuntimeException{

	private static final long serialVersionUID = 6753994776962949879L;

	
	public FatTransactionException() {
        super();
    }
	
	public FatTransactionException(String message) {
	    super(message);
	}
	
	public static FatTransactionException buildRemoteNodeErrorException(String errorServiceName) {
		return new FatTransactionException("remote node named " + errorServiceName + " error when local transaction runnning ");
	}
	
	public static void throwRemoteNodeErrorException(String errorServiceName) {
		throw buildRemoteNodeErrorException(errorServiceName);
	}
	
	public static boolean isFatException(Exception e) {
		return e instanceof FatTransactionException;
	}
	
}

package org.sokybot.exception;

public class SokybotException extends RuntimeException {

	
	public SokybotException(String message) {
      super(message) ; 
	}
	
	public SokybotException(String message , Throwable t) {
	   super(message , t) ;
	}
	
}

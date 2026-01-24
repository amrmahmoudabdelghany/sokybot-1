package org.sokybot.exception;

import lombok.Getter;

// TODO write docs 
@Getter
public class NameUniquenessConstraintViolationException extends SokybotException{

	private String targetName ; 
	
	
	public NameUniquenessConstraintViolationException(String message , String targetName) {
	    super(message) ; 
	}
}

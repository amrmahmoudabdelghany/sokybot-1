package org.sokybot.exception;

import lombok.Getter;
//TODO write docs 
@Getter
public class InvalidGameException extends SokybotException{

	private String gamePath ; 
	
	public InvalidGameException(String message , Throwable t , String gamePath) {

		super(message , t) ;
		this.gamePath = gamePath ; 
	}
	
}

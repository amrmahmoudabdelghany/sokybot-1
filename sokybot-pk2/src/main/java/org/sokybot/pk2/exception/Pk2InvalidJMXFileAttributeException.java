package org.sokybot.pk2.exception;

import org.sokybot.pk2.JMXFile;

import lombok.Getter;

/**
 * This exception will be thrown whenever the pk2 driver counter 
 * a joymax file with an invalid attributes like name , size , data position
 * 
 * @author Amr
 */
@Getter
public class Pk2InvalidJMXFileAttributeException extends RuntimeException {
 
	private String name ; 
	private long size; 
	private long position ;  
	
	public Pk2InvalidJMXFileAttributeException(String message  ,  String name , long size , long position ) {
	  super(message) ; 
	  
	  this.name = name ; 
	  this.size = size ; 
	  this.position = position ; 
	}
	
}

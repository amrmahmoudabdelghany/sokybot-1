package org.sokybot.exception;

import lombok.Getter;

/**
 * Thrown when deal with silkroad path that is not exists or  
 *  represents directory that missing the necessary files (*.pk2  , sro_client.exe ...etc) 
 * 
 * @author Amr
 */
@Getter
public class InvalidGameReferenceException extends SokybotException{

	private String targetPath ; 
	
	public InvalidGameReferenceException(String message  , String targetPath ) {
	    super(message) ;
	    this.targetPath = targetPath ; 
	}
	 
  
	
}

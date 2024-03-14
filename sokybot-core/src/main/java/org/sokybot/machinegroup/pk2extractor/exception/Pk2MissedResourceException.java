package org.sokybot.machinegroup.pk2extractor.exception;

/**
 * 
 * TODO write document
 * @author Amr
 */
public class Pk2MissedResourceException extends Pk2ExtractionException {

	
	public Pk2MissedResourceException(String message , String resourceName , Throwable t) {
		super(message , resourceName , t) ; 
	}
	
	public Pk2MissedResourceException(String message , String resourceName) {
	   super(message , resourceName ) ; 
	}
}

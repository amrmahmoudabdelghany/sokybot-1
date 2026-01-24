package org.sokybot.pk2.exception;

import java.io.IOException;
import java.io.UncheckedIOException;


import lombok.Getter;

/**
 * Pk2 file driver work with assumption that if the represented file is exists and
 *  channel is opened successfully then 
 * if any IOException occurs during reading operations that`s happens because the represented pk2 file is corrupted 
 * 
 * Objects from this exception class represents 
 * the event when the pk2 driver could not read pk2 entries properly
 * 
 * @author Amr
 */
@Getter 
public class Pk2FileCorruptedException extends UncheckedIOException {

	private String targetFile ; 
	private long readingPosition ; 
	
	public Pk2FileCorruptedException(String message  , String targetFile , long readingPos , IOException ex) {
	    super(message , ex) ; 
	    this.targetFile = targetFile ; 
	    this.readingPosition = readingPos ; 
	}
	
	
	
}

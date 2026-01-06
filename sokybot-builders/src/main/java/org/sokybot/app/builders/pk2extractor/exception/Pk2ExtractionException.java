import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
package org.sokybot.app.builders.pk2extractor.exception;

import lombok.Getter;

/**
 * 
 * This exception type designed to be a parent of any exception that occurs
 * during extraction operations , so catch this exception if you need to handle
 * any exception happens during extraction processes
 * 
 *  
 * 
 * @author Amr
 */
@Getter
public class Pk2ExtractionException extends RuntimeException {

	private String resourceName;

	public Pk2ExtractionException(String message, String resourceName, Throwable t) {
		super(message, t);
		this.resourceName = resourceName;

	}

	public Pk2ExtractionException(String message, String resourceName) {
		super(message);
		this.resourceName = resourceName;
	}

	public Pk2ExtractionException(String message, Throwable t) {
		super(message, t);
		this.resourceName = "JOYMAX_RESOURCE";
	}

	public Pk2ExtractionException(String message) {
		super(message);
	}

}





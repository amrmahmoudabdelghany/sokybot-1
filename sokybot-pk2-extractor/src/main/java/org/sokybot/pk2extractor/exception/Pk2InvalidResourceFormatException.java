package org.sokybot.pk2extractor.exception;

/**
 * Exception for invalid pk2 resource format.
 * 
 * @author Amr
 */
public class Pk2InvalidResourceFormatException extends Pk2ExtractionException {

    public Pk2InvalidResourceFormatException(String message, String resourceName, Throwable t) {
        super(message, resourceName, t);
    }

    public Pk2InvalidResourceFormatException(String message, String resourceName) {
        super(message, resourceName);
    }

    public Pk2InvalidResourceFormatException(String message, Throwable t) {
        super(message, t);
    }
}

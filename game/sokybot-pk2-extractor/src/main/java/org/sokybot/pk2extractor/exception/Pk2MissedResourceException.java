package org.sokybot.pk2extractor.exception;

/**
 * Exception for missing pk2 resources.
 * 
 * @author Amr
 */
public class Pk2MissedResourceException extends Pk2ExtractionException {

    public Pk2MissedResourceException(String message, String resourceName, Throwable t) {
        super(message, resourceName, t);
    }

    public Pk2MissedResourceException(String message, String resourceName) {
        super(message, resourceName);
    }
}

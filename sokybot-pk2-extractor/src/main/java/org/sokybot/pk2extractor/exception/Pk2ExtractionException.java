package org.sokybot.pk2extractor.exception;

import lombok.Getter;

/**
 * Base exception for pk2 extraction operations.
 * Catch this exception to handle any exception during extraction processes.
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

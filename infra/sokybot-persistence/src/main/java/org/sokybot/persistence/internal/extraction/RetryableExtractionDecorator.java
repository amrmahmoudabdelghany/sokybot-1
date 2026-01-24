package org.sokybot.persistence.internal.extraction;

import org.sokybot.persistence.service.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator that adds retry logic for failed extractions.
 * 
 * @param <T> The DTO type being extracted
 * @param <E> The entity type being persisted
 */
public class RetryableExtractionDecorator<T, E> extends ExtractionHandlerDecorator<T, E> {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryableExtractionDecorator.class);
    private final int maxRetries;
    private final long retryDelayMs;
    
    /**
     * Create a retryable decorator with default retry settings (3 retries, 1000ms delay).
     */
    public RetryableExtractionDecorator(IPk2ExtractionHandler<T, E> decorated) {
        this(decorated, 3, 1000);
    }
    
    /**
     * Create a retryable decorator with custom retry settings.
     * 
     * @param decorated The handler to wrap
     * @param maxRetries Maximum number of retry attempts (0 = no retries, only initial attempt)
     * @param retryDelayMs Delay in milliseconds between retry attempts
     */
    public RetryableExtractionDecorator(
            IPk2ExtractionHandler<T, E> decorated, 
            int maxRetries, 
            long retryDelayMs) {
        super(decorated);
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }
    
    @Override
    public void extractAndPersist(String gamePath, String pk2FileName) throws PersistenceException {
        int attempts = 0;
        PersistenceException lastException = null;
        
        while (attempts <= maxRetries) {
            try {
                decorated.extractAndPersist(gamePath, pk2FileName);
                if (attempts > 0) {
                    logger.info("Successfully extracted {} after {} retry attempts", 
                               getExtractionType(), attempts);
                }
                return;
            } catch (PersistenceException e) {
                lastException = e;
                attempts++;
                
                if (attempts <= maxRetries) {
                    logger.warn("Extraction failed (attempt {}/{}), retrying in {}ms...", 
                               attempts, maxRetries + 1, retryDelayMs);
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new PersistenceException("Retry interrupted", ie);
                    }
                }
            }
        }
        
        logger.error("Failed to extract {} after {} attempts", getExtractionType(), attempts);
        throw lastException;
    }
}
package org.sokybot.persistence.internal.extraction;

import org.sokybot.persistence.service.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;
import java.util.function.Supplier;

/**
 * Decorator that checks if extraction is needed (skips if already extracted).
 * Uses a supplier to check if data already exists in the database.
 * 
 * @param <T> The DTO type being extracted
 * @param <E> The entity type being persisted
 */
public class CachedExtractionDecorator<T, E> extends ExtractionHandlerDecorator<T, E> {
    
    private static final Logger logger = LoggerFactory.getLogger(CachedExtractionDecorator.class);
    
    private final Supplier<Boolean> hasExtracted;
    
    /**
     * Create a cached decorator that checks if extraction is needed.
     * 
     * @param decorated The handler to wrap
     * @param hasExtracted Supplier that returns true if data already exists (should skip extraction)
     */
    public CachedExtractionDecorator(
            IPk2ExtractionHandler<T, E> decorated,
            Supplier<Boolean> hasExtracted) {
        super(decorated);
        this.hasExtracted = hasExtracted;
    }
    
    @Override
    public void extractAndPersist(String gamePath, String pk2FileName) throws PersistenceException {
        if (Boolean.TRUE.equals(hasExtracted.get())) {
            logger.info("Skipping {} extraction - already extracted for game: {}", 
                       getExtractionType(), gamePath);
            return;
        }
        
        decorated.extractAndPersist(gamePath, pk2FileName);
    }
}
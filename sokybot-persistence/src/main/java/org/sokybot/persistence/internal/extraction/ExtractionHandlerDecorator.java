package org.sokybot.persistence.internal.extraction;

import org.sokybot.persistence.service.PersistenceException;

/**
 * Base decorator for IPk2ExtractionHandler.
 * Implements the Decorator pattern to add cross-cutting concerns.
 * 
 * @param <T> The DTO type being extracted
 * @param <E> The entity type being persisted
 */
public abstract class ExtractionHandlerDecorator<T, E> implements IPk2ExtractionHandler<T, E> {
    
    protected final IPk2ExtractionHandler<T, E> decorated;
    
    protected ExtractionHandlerDecorator(IPk2ExtractionHandler<T, E> decorated) {
        this.decorated = decorated;
    }
    
    @Override
    public void extractAndPersist(String gamePath, String pk2FileName) throws PersistenceException {
        decorated.extractAndPersist(gamePath, pk2FileName);
    }
    
    @Override
    public String getExtractionType() {
        return decorated.getExtractionType();
    }
}
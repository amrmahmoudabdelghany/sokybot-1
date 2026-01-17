package org.sokybot.persistence.internal.extraction;

import org.sokybot.persistence.service.PersistenceException;

/**
 * Handler for PK2 extraction operations.
 * Handles the extraction, conversion, and persistence of data from PK2 files.
 * 
 * @param <T> The DTO type being extracted (e.g., NPCData, ItemData)
 * @param <E> The entity type being persisted (e.g., NPCEntity, ItemEntity)
 */
public interface IPk2ExtractionHandler<T, E> {
    
    /**
     * Extract data from PK2 file and persist to database.
     * 
     * @param gamePath The game installation path
     * @param pk2FileName The PK2 file name (e.g., "Media.pk2")
     * @throws PersistenceException if extraction or persistence fails
     */
    void extractAndPersist(String gamePath, String pk2FileName) throws PersistenceException;
    
    /**
     * Get the type of data being extracted (for logging/debugging).
     */
    String getExtractionType();
}
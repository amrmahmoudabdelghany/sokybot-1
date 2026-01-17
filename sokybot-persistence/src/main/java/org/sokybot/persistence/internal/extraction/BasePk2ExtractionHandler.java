package org.sokybot.persistence.internal.extraction;

import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.persistence.service.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.io.File;
import java.io.IOException;

/**
 * Base implementation of IPk2ExtractionHandler.
 * Handles the common extraction flow: open PK2, run extractor, convert DTOs, persist entities.
 */
public abstract class BasePk2ExtractionHandler<T, E> implements IPk2ExtractionHandler<T, E> {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final EntityManagerFactory emf;
    private final org.sokybot.pk2extractor.IExtractor<T> extractor;
    
    protected BasePk2ExtractionHandler(
            EntityManagerFactory emf,
            org.sokybot.pk2extractor.IExtractor<T> extractor) {
        this.emf = emf;
        this.extractor = extractor;
    }
    
    @Override
    public void extractAndPersist(String gamePath, String pk2FileName) throws PersistenceException {
        String pk2Path = new File(gamePath, pk2FileName).getAbsolutePath();
        logger.info("Starting {} extraction from {} for game: {}", 
                   getExtractionType(), pk2FileName, gamePath);
        
        try (IPk2Driver driver = IPk2Driver.open(pk2Path)) {
            EntityManager em = emf.createEntityManager();
            EntityTransaction tx = em.getTransaction();
            
            try {
                tx.begin();
                final EntityManager emRef = em; // For lambda capture
                
                extractor.extract(driver, new ExtractionListener<T>() {
                    @Override
                    public void onExtracted(T dto) {
                        try {
                            E entity = convertDtoToEntity(dto);
                            if (entity != null) {
                                emRef.merge(entity);
                            }
                        } catch (Exception e) {
                            logger.error("Failed to convert/persist {}: {}", 
                                       getExtractionType(), dto, e);
                            // Continue processing other items
                        }
                    }
                    
                    @Override
                    public void onComplete(int totalCount) {
                        logger.info("Successfully extracted {} {}s for game: {}", 
                                   totalCount, getExtractionType(), gamePath);
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        logger.error("Extraction error during {} extraction for game: {}", 
                                   getExtractionType(), gamePath, error);
                        throw new RuntimeException(
                            "Extraction error: " + getExtractionType(), error);
                    }
                }, null);
                
                tx.commit();
                logger.info("Completed {} extraction for game: {}", getExtractionType(), gamePath);
                
            } catch (Exception e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                logger.error("Failed to extract {}s for game: {}", getExtractionType(), gamePath, e);
                throw new PersistenceException(
                    "Failed to extract " + getExtractionType(), e);
            } finally {
                em.close();
            }
            
        } catch (IOException e) {
            logger.error("Failed to open PK2 file: {} for game: {}", pk2FileName, gamePath, e);
            throw new PersistenceException(
                "Failed to open PK2 file: " + pk2FileName, e);
        }
    }
    
    /**
     * Convert DTO to entity. Subclasses implement this to provide type-specific conversion.
     */
    protected abstract E convertDtoToEntity(T dto);
}
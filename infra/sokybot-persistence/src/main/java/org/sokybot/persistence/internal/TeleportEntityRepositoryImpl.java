package org.sokybot.persistence.internal;

import org.sokybot.persistence.entities.TeleportEntity;
import org.sokybot.persistence.service.TeleportEntityRepository;

import javax.persistence.EntityManagerFactory;

/**
 * Implementation of TeleportEntityRepository using AbstractRepository base class.
 */
public class TeleportEntityRepositoryImpl extends AbstractRepository<TeleportEntity, Integer> 
        implements TeleportEntityRepository {
    
    /**
     */
    public TeleportEntityRepositoryImpl() {
        super(TeleportEntity.class);
    }
    
    /**
     * Constructor for programmatic creation with specific EMF.
     */
    public TeleportEntityRepositoryImpl(EntityManagerFactory emf) {
        super(emf, TeleportEntity.class);
    }
    
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }
    
    @Override
    protected String getIdFieldName() {
        return "refId";
    }
}
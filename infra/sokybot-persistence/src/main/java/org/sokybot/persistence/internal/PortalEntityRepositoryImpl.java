package org.sokybot.persistence.internal;

import org.sokybot.persistence.entities.PortalEntity;
import org.sokybot.persistence.service.PortalEntityRepository;

import javax.persistence.EntityManagerFactory;

/**
 * Implementation of PortalEntityRepository using AbstractRepository base class.
 */
public class PortalEntityRepositoryImpl extends AbstractRepository<PortalEntity, Integer> 
        implements PortalEntityRepository {
    
    /**
     */
    public PortalEntityRepositoryImpl() {
        super(PortalEntity.class);
    }
    
    /**
     * Constructor for programmatic creation with specific EMF.
     */
    public PortalEntityRepositoryImpl(EntityManagerFactory emf) {
        super(emf, PortalEntity.class);
    }
    
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }
    
    @Override
    protected String getIdFieldName() {
        return "refId";
    }
}
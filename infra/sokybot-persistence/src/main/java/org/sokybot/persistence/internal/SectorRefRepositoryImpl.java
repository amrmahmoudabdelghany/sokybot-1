package org.sokybot.persistence.internal;

import org.sokybot.persistence.entities.SectorRef;
import org.sokybot.persistence.service.SectorRefRepository;

import javax.persistence.EntityManagerFactory;

/**
 * Implementation of SectorRefRepository using AbstractRepository base class.
 */
public class SectorRefRepositoryImpl extends AbstractRepository<SectorRef, Short> 
        implements SectorRefRepository {
    
    /**
     */
    public SectorRefRepositoryImpl() {
        super(SectorRef.class);
    }
    
    /**
     * Constructor for programmatic creation with specific EMF.
     */
    public SectorRefRepositoryImpl(EntityManagerFactory emf) {
        super(emf, SectorRef.class);
    }
    
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }
    
    @Override
    protected String getIdFieldName() {
        return "sectorYX";
    }
}
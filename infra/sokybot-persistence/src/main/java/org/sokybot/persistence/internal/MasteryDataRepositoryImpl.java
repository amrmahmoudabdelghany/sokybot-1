package org.sokybot.persistence.internal;

import org.sokybot.persistence.entities.MasteryData;
import org.sokybot.persistence.service.MasteryDataRepository;

import javax.persistence.EntityManagerFactory;

/**
 * Implementation of MasteryDataRepository using AbstractRepository base class.
 */
public class MasteryDataRepositoryImpl extends AbstractRepository<MasteryData, Integer> 
        implements MasteryDataRepository {
    
    /**
     */
    public MasteryDataRepositoryImpl() {
        super(MasteryData.class);
    }
    
    /**
     * Constructor for programmatic creation with specific EMF.
     */
    public MasteryDataRepositoryImpl(EntityManagerFactory emf) {
        super(emf, MasteryData.class);
    }
    
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }
    
    @Override
    protected String getIdFieldName() {
        return "masteryId";
    }
}
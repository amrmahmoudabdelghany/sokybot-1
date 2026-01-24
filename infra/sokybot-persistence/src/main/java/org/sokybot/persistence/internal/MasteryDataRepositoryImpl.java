package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.MasteryData;
import org.sokybot.persistence.service.MasteryDataRepository;

import javax.persistence.EntityManagerFactory;

/**
 * Implementation of MasteryDataRepository using AbstractRepository base class.
 */
@Component(service = MasteryDataRepository.class)
public class MasteryDataRepositoryImpl extends AbstractRepository<MasteryData, Integer> 
        implements MasteryDataRepository {
    
    /**
     * OSGi DS default constructor - will be injected via @Reference.
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
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }
    
    @Override
    protected String getIdFieldName() {
        return "masteryId";
    }
}
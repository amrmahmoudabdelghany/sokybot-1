package org.sokybot.persistence.internal;

import org.sokybot.persistence.entities.LvlEXP;
import org.sokybot.persistence.service.LvlEXPRepository;

import javax.persistence.EntityManagerFactory;

/**
 * Implementation of LvlEXPRepository using AbstractRepository base class.
 */
public class LvlEXPRepositoryImpl extends AbstractRepository<LvlEXP, Integer> 
        implements LvlEXPRepository {
    
    /**
     */
    public LvlEXPRepositoryImpl() {
        super(LvlEXP.class);
    }
    
    /**
     * Constructor for programmatic creation with specific EMF.
     */
    public LvlEXPRepositoryImpl(EntityManagerFactory emf) {
        super(emf, LvlEXP.class);
    }
    
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }
    
    @Override
    protected String getIdFieldName() {
        return "level";
    }
}
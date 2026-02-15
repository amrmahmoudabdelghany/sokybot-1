package org.sokybot.persistence.internal;

import org.sokybot.persistence.entities.SkillEntity;
import org.sokybot.persistence.service.SkillEntityRepository;

import javax.persistence.EntityManagerFactory;

/**
 * Implementation of SkillEntityRepository using AbstractRepository base class.
 */
public class SkillEntityRepositoryImpl extends AbstractRepository<SkillEntity, Integer> 
        implements SkillEntityRepository {
    
    /**
     */
    public SkillEntityRepositoryImpl() {
        super(SkillEntity.class);
    }
    
    /**
     * Constructor for programmatic creation with specific EMF.
     */
    public SkillEntityRepositoryImpl(EntityManagerFactory emf) {
        super(emf, SkillEntity.class);
    }
    
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }
    
    @Override
    protected String getIdFieldName() {
        return "refId";
    }
}
package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.SkillEntity;
import org.sokybot.persistence.service.SkillEntityRepository;

import javax.persistence.EntityManagerFactory;

/**
 * Implementation of SkillEntityRepository using AbstractRepository base class.
 */
@Component(service = SkillEntityRepository.class)
public class SkillEntityRepositoryImpl extends AbstractRepository<SkillEntity, Integer> 
        implements SkillEntityRepository {
    
    /**
     * OSGi DS default constructor - will be injected via @Reference.
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
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }
    
    @Override
    protected String getIdFieldName() {
        return "refId";
    }
}
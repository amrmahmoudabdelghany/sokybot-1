package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.service.NPCEntityRepository;

import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Implementation of NPCEntityRepository using AbstractRepository base class.
 */
@Component(service = NPCEntityRepository.class)
public class NPCEntityRepositoryImpl extends AbstractRepository<NPCEntity, Integer> 
        implements NPCEntityRepository {
    
    /**
     * OSGi DS default constructor - will be injected via @Reference.
     */
    public NPCEntityRepositoryImpl() {
        super(NPCEntity.class);
    }
    
    /**
     * Constructor for programmatic creation with specific EMF.
     */
    public NPCEntityRepositoryImpl(EntityManagerFactory emf) {
        super(emf, NPCEntity.class);
    }
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }
    
    @Override
    protected String getIdFieldName() {
        return "refId";
    }
    
    @Override
    public List<NPCEntity> findAllMonsterLike(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return findAll();
        }
        return executeReadOnly(em -> {
            TypedQuery<NPCEntity> query = em.createQuery(
                "SELECT n FROM NPCEntity n WHERE lower(n.name) LIKE lower(:filter)", NPCEntity.class);
            query.setParameter("filter", "%" + filter + "%");
            return query.getResultList();
        });
    }
}
package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.service.ItemEntityRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ItemEntityRepository using AbstractRepository base class.
 */
@Component(service = ItemEntityRepository.class)
public class ItemEntityRepositoryImpl extends AbstractRepository<ItemEntity, Integer> 
        implements ItemEntityRepository {
    
    /**
     * OSGi DS default constructor - will be injected via @Reference.
     */
    public ItemEntityRepositoryImpl() {
        super(ItemEntity.class);
    }
    
    /**
     * Constructor for programmatic creation with specific EMF.
     */
    public ItemEntityRepositoryImpl(EntityManagerFactory emf) {
        super(emf, ItemEntity.class);
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
    public Optional<ItemEntity> findItemEntityByLongId(String longId) {
        if (longId == null) {
            return Optional.empty();
        }
        return executeReadOnly(em -> {
            List<ItemEntity> results = em.createQuery(
                "SELECT i FROM ItemEntity i WHERE i.longId = :longId", ItemEntity.class)
                .setParameter("longId", longId)
                .getResultList();
            return results.isEmpty() ? Optional.<ItemEntity>empty() : Optional.of(results.get(0));
        });
    }
}
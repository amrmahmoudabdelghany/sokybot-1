package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.ShopEntity;
import org.sokybot.persistence.service.ShopEntityRepository;

import javax.persistence.EntityManagerFactory;

/**
 * Implementation of ShopEntityRepository using AbstractRepository base class.
 */
@Component(service = ShopEntityRepository.class)
public class ShopEntityRepositoryImpl extends AbstractRepository<ShopEntity, Integer> 
        implements ShopEntityRepository {
    
    /**
     * OSGi DS default constructor - will be injected via @Reference.
     */
    public ShopEntityRepositoryImpl() {
        super(ShopEntity.class);
    }
    
    /**
     * Constructor for programmatic creation with specific EMF.
     */
    public ShopEntityRepositoryImpl(EntityManagerFactory emf) {
        super(emf, ShopEntity.class);
    }
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }
    
    @Override
    protected String getIdFieldName() {
        return "shopId";
    }
}

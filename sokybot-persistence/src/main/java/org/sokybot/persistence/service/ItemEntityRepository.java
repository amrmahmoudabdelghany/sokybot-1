package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.ItemEntity;
import java.util.List;
import java.util.Optional;

/**
 * OSGi service interface for ItemEntity persistence operations.
 */
public interface ItemEntityRepository {
    
    ItemEntity save(ItemEntity entity);
    
    List<ItemEntity> saveAll(List<ItemEntity> entities);
    
    Optional<ItemEntity> findById(Integer id);
    
    List<ItemEntity> findAll();
    
    void deleteById(Integer id);
    
    void delete(ItemEntity entity);
    
    void deleteAll();
    
    boolean existsById(Integer id);
    
    long count();
    
    /**
     * Find item by longId.
     */
    Optional<ItemEntity> findItemEntityByLongId(String longId);
}

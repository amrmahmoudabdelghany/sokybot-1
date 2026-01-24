package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.ItemEntity;
import java.util.Optional;

/**
 * OSGi service interface for ItemEntity persistence operations.
 */
public interface ItemEntityRepository extends IRepository<ItemEntity, Integer> {
    
    /**
     * Find item by longId.
     */
    Optional<ItemEntity> findItemEntityByLongId(String longId);
}

package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.NPCEntity;
import java.util.List;

/**
 * OSGi service interface for NPCEntity persistence operations.
 */
public interface NPCEntityRepository extends IRepository<NPCEntity, Integer> {
    
    /**
     * Find all monsters matching filter (level or name).
     */
    List<NPCEntity> findAllMonsterLike(String filter);
}

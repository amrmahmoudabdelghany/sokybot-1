package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.NPCEntity;
import java.util.List;
import java.util.Optional;

/**
 * OSGi service interface for NPCEntity persistence operations.
 */
public interface NPCEntityRepository {
    
    NPCEntity save(NPCEntity entity);
    
    List<NPCEntity> saveAll(List<NPCEntity> entities);
    
    Optional<NPCEntity> findById(Integer id);
    
    List<NPCEntity> findAll();
    
    void deleteById(Integer id);
    
    void delete(NPCEntity entity);
    
    void deleteAll();
    
    boolean existsById(Integer id);
    
    long count();
    
    /**
     * Find all monsters matching filter (level or name).
     */
    List<NPCEntity> findAllMonsterLike(String filter);
}

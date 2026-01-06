package org.sokybot.persistence.service;

import org.sokybot.app.domain.GroupInfo;
import java.util.List;
import java.util.Optional;

/**
 * OSGi service interface for GroupInfo persistence operations.
 * Replaces Spring Data's CrudRepository pattern.
 */
public interface GroupInfoRepository {
    
    /**
     * Saves a GroupInfo entity (insert or update).
     */
    GroupInfo save(GroupInfo entity);
    
    /**
     * Finds a GroupInfo by its ID.
     */
    Optional<GroupInfo> findById(Integer id);
    
    /**
     * Returns all GroupInfo entities.
     */
    List<GroupInfo> findAll();
    
    /**
     * Deletes a GroupInfo by its ID.
     */
    void deleteById(Integer id);
    
    /**
     * Deletes a GroupInfo entity.
     */
    void delete(GroupInfo entity);
    
    /**
     * Checks if a GroupInfo exists by ID.
     */
    boolean existsById(Integer id);
    
    /**
     * Returns the total count of GroupInfo entities.
     */
    long count();
    
    // Custom query methods
    
    /**
     * Finds a GroupInfo by unique name.
     */
    Optional<GroupInfo> findByName(String name);
    
    /**
     * Finds a GroupInfo by unique game path.
     */
    Optional<GroupInfo> findByGamePath(String gamePath);
}

package org.sokybot.runtime.internal.persistence;

import org.sokybot.runtime.internal.domain.GroupInfo;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GroupInfo persistence operations.
 * Internal use only within sokybot-runtime.
 */
public interface GroupInfoRepository {

    /**
     * Saves a GroupInfo entity (insert or update).
     */
    GroupInfo save(GroupInfo entity);

    /**
     * Finds a GroupInfo by its ID.
     */
    Optional<GroupInfo> findById(int id);

    /**
     * Returns all GroupInfo entities.
     */
    List<GroupInfo> findAll();

    /**
     * Deletes a GroupInfo by its ID.
     */
    void deleteById(int id);

    /**
     * Deletes a GroupInfo entity.
     */
    void delete(GroupInfo entity);

    /**
     * Checks if a GroupInfo exists by ID.
     */
    boolean existsById(int id);

    /**
     * Returns the total count of GroupInfo entities.
     */
    long count();

    /**
     * Finds a GroupInfo by unique name.
     */
    Optional<GroupInfo> findByName(String name);

    /**
     * Finds a GroupInfo by unique game path.
     */
    Optional<GroupInfo> findByGamePath(String gamePath);
}

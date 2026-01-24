package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.TeleportEntity;

/**
 * OSGi service interface for TeleportEntity persistence operations.
 */
public interface TeleportEntityRepository extends IRepository<TeleportEntity, Integer> {
    // Additional specific methods can be added here if needed
}

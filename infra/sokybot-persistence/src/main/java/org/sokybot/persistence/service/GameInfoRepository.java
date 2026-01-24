package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.GameInfo;

/**
 * OSGi service interface for GameInfo persistence operations.
 */
public interface GameInfoRepository extends IRepository<GameInfo, String> {
    // Additional specific methods can be added here if needed
}

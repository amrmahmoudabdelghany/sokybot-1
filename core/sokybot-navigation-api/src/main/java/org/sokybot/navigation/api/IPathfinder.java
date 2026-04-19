package org.sokybot.navigation.api;

import java.util.List;

/**
 * Terrain-aware path queries over the navigation mesh (implemented by the game-navigation bundle).
 */
public interface IPathfinder {

    boolean isReachable(WorldPoint origin, WorldPoint destination);

    List<WorldPoint> findPath(WorldPoint origin, WorldPoint destination);
}

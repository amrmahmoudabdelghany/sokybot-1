package org.sokybot.game.navigation;

import java.util.List;

import org.sokybot.persistence.entities.geo.Vector2D;
import org.sokybot.persistence.entities.navmesh.Position;

/**
 * Interface for finding paths/routes in the game world.
 * Implementations use A* pathfinding over the navigation mesh.
 */
public interface IRouteFinder {

    /**
     * Find a path between two points.
     * 
     * @param startX start X coordinate (world coordinates)
     * @param startY start Y coordinate (world coordinates)
     * @param stopX  end X coordinate (world coordinates)
     * @param stopY  end Y coordinate (world coordinates)
     * @param h      heuristic weight for A* algorithm
     * @return list of waypoints from start to end
     */
    List<Vector2D> findPath(float startX, float startY, float stopX, float stopY, float h);

    /**
     * Find a path between two positions.
     * 
     * @param start start position
     * @param stop  end position
     * @param h     heuristic weight for A* algorithm
     * @return list of waypoints from start to end
     */
    List<Vector2D> findPath(Position start, Position stop, float h);
}

package org.sokybot.game.navigation.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.game.navigation.IRouteFinder;
import org.sokybot.game.navigation.IRouteFinderFactory;
import org.sokybot.navigation.api.IPathfinder;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.persistence.entities.geo.Vector2D;
import org.sokybot.persistence.service.IGameDataLookup;

@Component(service = IPathfinder.class, immediate = true)
public final class PathfinderImpl implements IPathfinder {

    private static final float DEFAULT_HEURISTIC = 1.0f;

    @Reference
    private IGameDataLookup gameDataLookup;

    @Reference
    private IRouteFinderFactory routeFinderFactory;

    private volatile IRouteFinder routeFinder;

    @Activate
    void activate() {
        this.routeFinder = routeFinderFactory.createRouteFinder(gameDataLookup);
    }

    @Override
    public List<WorldPoint> findPath(WorldPoint origin, WorldPoint destination) {
        if (origin == null || destination == null || routeFinder == null) {
            return Collections.emptyList();
        }
        try {
            List<Vector2D> raw = routeFinder.findPath(origin.getX(), origin.getY(), destination.getX(),
                    destination.getY(), DEFAULT_HEURISTIC);
            if (raw == null || raw.isEmpty()) {
                return Collections.emptyList();
            }
            List<WorldPoint> out = new ArrayList<>(raw.size());
            for (int i = raw.size() - 1; i >= 0; i--) {
                Vector2D v = raw.get(i);
                out.add(new WorldPoint((float) v.x, (float) v.y, destination.getZ()));
            }
            return out;
        } catch (RuntimeException ex) {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isReachable(WorldPoint origin, WorldPoint destination) {
        return !findPath(origin, destination).isEmpty();
    }
}

package org.sokybot.game.navigation.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.game.navigation.IRouteFinder;
import org.sokybot.game.navigation.IRouteFinderFactory;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Factory implementation for creating RouteFinder instances.
 */
@Component(service = IRouteFinderFactory.class)
public class RouteFinderFactoryImpl implements IRouteFinderFactory {

    @Override
    public IRouteFinder createRouteFinder(IGameDataLookup gameDataLookup) {
        NavMesh navMesh = new NavMesh(gameDataLookup);
        return new RouteFinder(navMesh);
    }
}

package org.sokybot.topology.api;

import java.util.Optional;

import org.sokybot.navigation.api.WorldPoint;

public interface ITeleportRouter {

    Optional<TeleportRoute> route(WorldPoint from, WorldPoint to);
}

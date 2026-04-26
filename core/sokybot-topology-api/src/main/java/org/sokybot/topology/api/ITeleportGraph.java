package org.sokybot.topology.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.sokybot.navigation.api.WorldPoint;

public interface ITeleportGraph {

    Collection<TeleportNode> nodes();

    List<TeleportEdge> edgesFrom(int npcRefId);

    Optional<TeleportNode> nearestNode(WorldPoint pos);

    Optional<TeleportNode> nearestNodeWithin(WorldPoint pos, float maxWorldUnits);

    long generation();
}

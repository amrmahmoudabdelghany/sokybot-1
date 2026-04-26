package org.sokybot.swarm.api;

import java.util.Collections;
import java.util.Set;

public interface ISwarmRadarPolicy {

    default Set<Integer> watchedRefIds() {
        return Collections.emptySet();
    }

    default long dedupeCooldownMs() {
        return 30_000L;
    }

    default boolean broadcastVisualConfirms() {
        return true;
    }
}

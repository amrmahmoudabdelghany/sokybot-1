package org.sokybot.behaviors.swarm.caravan;

import java.util.Optional;

import org.sokybot.swarm.api.SwarmCaravanTelemetryEvent;

/**
 * Epic #18: JVM-wide latest caravan telemetry per logical caravan id (populated by {@link CaravanTelemetryListener}).
 */
public interface ICaravanTelemetryCache {

    void put(String caravanId, SwarmCaravanTelemetryEvent event);

    Optional<SwarmCaravanTelemetryEvent> getLatest(String caravanId);
}

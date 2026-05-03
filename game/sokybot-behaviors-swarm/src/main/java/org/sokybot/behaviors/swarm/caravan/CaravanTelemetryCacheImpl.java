package org.sokybot.behaviors.swarm.caravan;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.sokybot.swarm.api.SwarmCaravanTelemetryEvent;

@Component(service = ICaravanTelemetryCache.class, immediate = true)
public final class CaravanTelemetryCacheImpl implements ICaravanTelemetryCache {

    private final ConcurrentHashMap<String, SwarmCaravanTelemetryEvent> latestByCaravanId = new ConcurrentHashMap<>();

    @Override
    public void put(String caravanId, SwarmCaravanTelemetryEvent event) {
        if (caravanId == null || event == null) {
            return;
        }
        String key = caravanId.trim();
        if (key.isEmpty()) {
            return;
        }
        latestByCaravanId.put(key, event);
    }

    @Override
    public Optional<SwarmCaravanTelemetryEvent> getLatest(String caravanId) {
        if (caravanId == null) {
            return Optional.empty();
        }
        String key = caravanId.trim();
        if (key.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(latestByCaravanId.get(key));
    }
}

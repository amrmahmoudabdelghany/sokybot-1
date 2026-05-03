package org.sokybot.behaviors.swarm.caravan;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmCaravanTelemetryEvent;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #18: fills {@link ICaravanTelemetryCache} from swarm caravan telemetry events.
 */
@Component(immediate = true)
public final class CaravanTelemetryListener {

    private static final Logger log = LoggerFactory.getLogger(CaravanTelemetryListener.class);

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference
    private ICaravanTelemetryCache caravanTelemetryCache;

    private volatile Disposable telemetrySub;

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmBus;
        ICaravanTelemetryCache cache = caravanTelemetryCache;
        if (bus == null || cache == null) {
            log.warn("CaravanTelemetryListener: missing ISwarmEventBus or ICaravanTelemetryCache");
            return;
        }
        telemetrySub = bus.observe(SwarmCaravanTelemetryEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "CaravanTelemetryListener stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(evt -> cache.put(evt.getCaravanId(), evt));
    }

    @Deactivate
    void deactivate() {
        Disposable d = telemetrySub;
        telemetrySub = null;
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }
}

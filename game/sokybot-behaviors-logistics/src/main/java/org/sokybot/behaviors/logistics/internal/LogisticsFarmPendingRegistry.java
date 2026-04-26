package org.sokybot.behaviors.logistics.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.LogisticsAbortedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks pending farmer logistics requests so bundle shutdown can broadcast {@link LogisticsAbortedEvent}.
 */
@Component(service = LogisticsFarmPendingRegistry.class, immediate = true)
public final class LogisticsFarmPendingRegistry {

    private static final Logger log = LoggerFactory.getLogger(LogisticsFarmPendingRegistry.class);

    private final Map<String, String> pendingRequestByFarmer = new ConcurrentHashMap<>();

    @Reference
    private volatile ISwarmEventBus swarmBus;

    @Activate
    void activate() {
        log.debug("LogisticsFarmPendingRegistry active");
    }

    @Deactivate
    void deactivate() {
        ISwarmEventBus bus = swarmBus;
        long now = System.currentTimeMillis();
        for (Map.Entry<String, String> e : pendingRequestByFarmer.entrySet()) {
            if (bus != null) {
                bus.publish(new LogisticsAbortedEvent(
                        e.getKey(), now, e.getValue(), LogisticsAbortedEvent.Reason.FARMER_DISCONNECTED));
                bus.releaseClaim(e.getValue());
            }
        }
        pendingRequestByFarmer.clear();
    }

    public void rememberPending(String farmerMachineId, String requestId) {
        if (farmerMachineId == null || requestId == null) {
            return;
        }
        pendingRequestByFarmer.put(farmerMachineId.trim(), requestId.trim());
    }

    public void clearPending(String farmerMachineId) {
        if (farmerMachineId == null) {
            return;
        }
        pendingRequestByFarmer.remove(farmerMachineId.trim());
    }
}

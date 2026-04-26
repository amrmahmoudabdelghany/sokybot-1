package org.sokybot.behaviors.swarm.internal;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.gameevents.events.entity.EntityDespawnEvent;
import org.sokybot.swarm.api.LogisticsAbortedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;

/**
 * Correlates abort signals and farmer despawn for active mule swarm jobs (per mule machine id).
 */
@Component(service = SwarmJobMonitor.class, immediate = true)
public final class SwarmJobMonitor {

    private static final Logger log = LoggerFactory.getLogger(SwarmJobMonitor.class);

    private final ConcurrentHashMap<String, ActiveJob> jobsByMule = new ConcurrentHashMap<>();

    @Reference
    private IReactiveEventBus reactiveEventBus;

    private Disposable abortSub;
    private Disposable despawnSub;

    @Activate
    void activate() {
        abortSub = reactiveEventBus
                .on(LogisticsAbortedEvent.class)
                .subscribe(this::onAborted);
        despawnSub = reactiveEventBus
                .on(EntityDespawnEvent.class)
                .subscribe(this::onDespawn);
    }

    @Deactivate
    void deactivate() {
        dispose(abortSub);
        dispose(despawnSub);
        jobsByMule.clear();
    }

    private static void dispose(Disposable d) {
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    public void registerJob(String muleMachineId, String requestId, int farmerSpawnUid) {
        if (muleMachineId == null || requestId == null) {
            return;
        }
        jobsByMule.put(
                muleMachineId.trim(),
                new ActiveJob(requestId.trim(), farmerSpawnUid));
    }

    public void clearJob(String muleMachineId) {
        if (muleMachineId != null) {
            jobsByMule.remove(muleMachineId.trim());
        }
    }

    /**
     * @return abort reason to act on, or null if none pending
     */
    public LogisticsAbortedEvent.Reason pollAbortReason(String muleMachineId) {
        if (muleMachineId == null) {
            return null;
        }
        ActiveJob j = jobsByMule.get(muleMachineId.trim());
        if (j == null) {
            return null;
        }
        return j.pendingReason.getAndSet(null);
    }

    private void onAborted(LogisticsAbortedEvent e) {
        for (Map.Entry<String, ActiveJob> ent : jobsByMule.entrySet()) {
            if (ent.getValue().requestId.equals(e.getRequestId())) {
                ent.getValue().pendingReason.compareAndSet(null, e.getReason());
            }
        }
    }

    private void onDespawn(EntityDespawnEvent e) {
        String machine = e.getFullName();
        ActiveJob j = jobsByMule.get(machine);
        if (j == null) {
            return;
        }
        if (j.farmerSpawnUid != 0 && e.getEntityId() == j.farmerSpawnUid) {
            j.pendingReason.compareAndSet(null, LogisticsAbortedEvent.Reason.FARMER_DISCONNECTED);
            log.info("Farmer entity {} despawned for mule job {}", e.getEntityId(), machine);
        }
    }

    private static final class ActiveJob {
        final String requestId;
        final int farmerSpawnUid;
        final AtomicReference<LogisticsAbortedEvent.Reason> pendingReason = new AtomicReference<>();

        ActiveJob(String requestId, int farmerSpawnUid) {
            this.requestId = Objects.requireNonNull(requestId);
            this.farmerSpawnUid = farmerSpawnUid;
        }
    }
}

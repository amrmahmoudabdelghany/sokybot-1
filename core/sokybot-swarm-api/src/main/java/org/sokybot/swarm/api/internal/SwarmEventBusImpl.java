package org.sokybot.swarm.api.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.LogisticsAbortedEvent;
import org.sokybot.swarm.api.LogisticsClaimedEvent;
import org.sokybot.swarm.api.LogisticsCompletedEvent;
import org.sokybot.swarm.api.LogisticsRequestEvent;
import org.sokybot.swarm.api.SwarmEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;

/**
 * In-process swarm bus: indexes open requests, CAS claims, and TTL / stale-claim janitor.
 */
@Component(service = ISwarmEventBus.class, immediate = true)
public final class SwarmEventBusImpl implements ISwarmEventBus {

    private static final Logger log = LoggerFactory.getLogger(SwarmEventBusImpl.class);

    private static final long STALE_CLAIM_MS = 90_000L;

    @Reference
    private IReactiveEventBus underlying;

    private final ConcurrentMap<String, ClaimRecord> claims = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LogisticsRequestEvent> open = new ConcurrentHashMap<>();
    private final ScheduledExecutorService janitor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "swarm-bus-janitor");
                t.setDaemon(true);
                return t;
            });

    @Activate
    void activate() {
        janitor.scheduleAtFixedRate(this::sweep, 5, 5, TimeUnit.SECONDS);
    }

    @Deactivate
    void deactivate() {
        janitor.shutdown();
        try {
            if (!janitor.awaitTermination(3, TimeUnit.SECONDS)) {
                janitor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            janitor.shutdownNow();
        }
        claims.clear();
        open.clear();
    }

    @Override
    public <E extends SwarmEvent> Flux<E> observe(Class<E> type) {
        return underlying.on(type);
    }

    @Override
    public void publish(SwarmEvent event) {
        if (event == null) {
            return;
        }
        if (event instanceof LogisticsRequestEvent) {
            LogisticsRequestEvent req = (LogisticsRequestEvent) event;
            open.put(req.getRequestId(), req);
        }
        if (event instanceof LogisticsCompletedEvent || event instanceof LogisticsAbortedEvent) {
            open.remove(event.getRequestId());
            claims.remove(event.getRequestId());
        }
        underlying.publish(event);
    }

    @Override
    public boolean tryClaim(String requestId, String muleMachineId, long muleTrainerUniqueId) {
        if (requestId == null || requestId.trim().isEmpty() || muleMachineId == null || muleMachineId.trim().isEmpty()) {
            return false;
        }
        String rid = requestId.trim();
        String mid = muleMachineId.trim();
        ClaimRecord prev = claims.putIfAbsent(rid, new ClaimRecord(mid, muleTrainerUniqueId, System.currentTimeMillis()));
        if (prev != null) {
            return false;
        }
        LogisticsRequestEvent req = open.get(rid);
        if (req != null) {
            publish(new LogisticsClaimedEvent(
                    req.getRequesterMachineId(), System.currentTimeMillis(), rid, mid, muleTrainerUniqueId));
        }
        return true;
    }

    @Override
    public Optional<String> getClaimHolderMachineId(String requestId) {
        if (requestId == null) {
            return Optional.empty();
        }
        ClaimRecord c = claims.get(requestId.trim());
        return c == null ? Optional.empty() : Optional.of(c.muleMachineId);
    }

    @Override
    public OptionalLong getClaimMuleTrainerUniqueId(String requestId) {
        if (requestId == null) {
            return OptionalLong.empty();
        }
        ClaimRecord c = claims.get(requestId.trim());
        if (c == null) {
            return OptionalLong.empty();
        }
        return c.muleTrainerUniqueId == 0L ? OptionalLong.empty() : OptionalLong.of(c.muleTrainerUniqueId);
    }

    @Override
    public List<LogisticsRequestEvent> openRequests() {
        long now = System.currentTimeMillis();
        List<LogisticsRequestEvent> snap = new ArrayList<>(open.values());
        List<LogisticsRequestEvent> list = new ArrayList<>();
        for (LogisticsRequestEvent r : snap) {
            if (r == null) {
                continue;
            }
            if (r.getExpiresAtEpochMs() > now && !claims.containsKey(r.getRequestId())) {
                list.add(r);
            }
        }
        list.sort(Comparator
                .comparing(LogisticsRequestEvent::getPriority, Comparator.reverseOrder())
                .thenComparingLong(LogisticsRequestEvent::getTimestampEpochMs));
        return list;
    }

    @Override
    public void releaseClaim(String requestId) {
        if (requestId == null) {
            return;
        }
        claims.remove(requestId.trim());
    }

    void sweep() {
        try {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, LogisticsRequestEvent> e : open.entrySet()) {
                String rid = e.getKey();
                LogisticsRequestEvent r = e.getValue();
                if (r == null) {
                    continue;
                }
                if (r.getExpiresAtEpochMs() <= now) {
                    open.remove(rid, r);
                    claims.remove(rid);
                    underlying.publish(new LogisticsAbortedEvent(
                            r.getRequesterMachineId(), now, rid, LogisticsAbortedEvent.Reason.TIMEOUT));
                }
            }
            for (Map.Entry<String, ClaimRecord> e : claims.entrySet()) {
                String rid = e.getKey();
                ClaimRecord c = e.getValue();
                if (c == null) {
                    continue;
                }
                if (!open.containsKey(rid) && (now - c.claimedAtEpochMs) > STALE_CLAIM_MS) {
                    if (claims.remove(rid, c)) {
                        log.debug("Removed stale swarm claim for requestId={} mule={}", rid, c.muleMachineId);
                    }
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Swarm bus janitor sweep error", ex);
        }
    }

    private static final class ClaimRecord {
        final String muleMachineId;
        final long muleTrainerUniqueId;
        final long claimedAtEpochMs;

        ClaimRecord(String muleMachineId, long muleTrainerUniqueId, long claimedAtEpochMs) {
            this.muleMachineId = muleMachineId;
            this.muleTrainerUniqueId = muleTrainerUniqueId;
            this.claimedAtEpochMs = claimedAtEpochMs;
        }
    }
}

package org.sokybot.swarm.api;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import reactor.core.publisher.Flux;

/**
 * JVM-wide swarm logistics bus: typed observe/publish on top of {@link org.sokybot.commons.event.IReactiveEventBus}
 * plus single-winner {@link #tryClaim(String, String)} semantics.
 */
public interface ISwarmEventBus {

    /** Type-filtered hot stream (same contract as {@code IReactiveEventBus#on}). */
    <E extends SwarmEvent> Flux<E> observe(Class<E> type);

    /** Publish any swarm event (claim arbitration is handled by {@link #tryClaim}). */
    void publish(SwarmEvent event);

    /**
     * Atomic claim: at most one successful claimant per {@code requestId} (CAS via
     * {@link java.util.concurrent.ConcurrentHashMap#putIfAbsent(Object, Object)}).
     * On success, a {@link LogisticsClaimedEvent} is published when the open request is still indexed.
     */
    default boolean tryClaim(String requestId, String muleMachineId) {
        return tryClaim(requestId, muleMachineId, 0L);
    }

    /**
     * Same as {@link #tryClaim(String, String)}; {@code muleTrainerUniqueId} is included on
     * {@link LogisticsClaimedEvent} so farmers can correlate {@code TradeWindowOpened} / exchange UI.
     */
    boolean tryClaim(String requestId, String muleMachineId, long muleTrainerUniqueId);

    /** Active claimant's machine id for a request, if any. */
    Optional<String> getClaimHolderMachineId(String requestId);

    /** Mule trainer unique id recorded at claim time, if known. */
    OptionalLong getClaimMuleTrainerUniqueId(String requestId);

    /** Snapshot of unclaimed open requests (priority desc, then age asc). */
    List<LogisticsRequestEvent> openRequests();

    /** Release a claim without completing the job (abort / timeout path). */
    void releaseClaim(String requestId);

    /**
     * Global radar dedupe gate for refId announcements.
     *
     * @return {@code true} when this caller is allowed to publish now
     */
    boolean tryAnnounceRefId(int refId, long cooldownMs);
}

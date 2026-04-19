package org.sokybot.session.api;

import java.util.Optional;

import reactor.core.publisher.Flux;

/**
 * OSGi service contract for observing session lifecycle state.
 * <p>
 * Provides both point-in-time snapshots and reactive streams of session
 * state changes, sampled at 50ms to avoid excessive downstream pressure.
 * <p>
 * Implementation lives in the session projections bundle.
 */
public interface ISessionModel {

    /**
     * Returns the current session snapshot for the given machine, if available.
     *
     * @param machineId the machine identifier
     * @return snapshot or empty if the machine has no session state yet
     */
    Optional<ISessionSnapshot> snapshot(String machineId);

    /**
     * Returns a reactive stream of session snapshots for the given machine.
     * The stream starts with the current snapshot (seed) and pushes updates,
     * sampled at 50ms.
     *
     * @param machineId the machine identifier
     * @return a Flux emitting session state changes
     */
    Flux<ISessionSnapshot> observe(String machineId);

    /**
     * Returns a reactive stream of session snapshots for all machines.
     * Useful for multi-machine monitoring UIs.
     *
     * @return a Flux emitting session state changes across all machines
     */
    Flux<ISessionSnapshot> observeAll();
}

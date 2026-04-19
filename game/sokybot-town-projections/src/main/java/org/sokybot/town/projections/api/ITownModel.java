package org.sokybot.town.projections.api;

import java.util.Optional;

import org.sokybot.town.api.ITownSnapshot;

import reactor.core.publisher.Flux;

/**
 * Tactical town overlay per bot (machine full name {@code group.machineName}).
 */
public interface ITownModel {

    /**
     * Latest immutable snapshot for the machine, if any town-related state exists.
     */
    Optional<ITownSnapshot> snapshot(String machineFullName);

    /**
     * Stream of snapshots sampled at a modest fixed interval while subscribed.
     */
    Flux<ITownSnapshot> observe(String machineFullName);
}

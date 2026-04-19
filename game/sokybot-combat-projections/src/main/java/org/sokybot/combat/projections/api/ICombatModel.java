package org.sokybot.combat.projections.api;

import java.util.Optional;

import org.sokybot.combat.api.ICombatSnapshot;

import reactor.core.publisher.Flux;

/**
 * Tactical combat overlay per bot (machine full name {@code group.machineName}).
 */
public interface ICombatModel {

    /**
     * Latest immutable snapshot for the machine, if any tactical state exists.
     */
    Optional<ICombatSnapshot> snapshot(String machineFullName);

    /**
     * Stream of snapshots sampled at a modest fixed interval while subscribed (push-style sampling).
     */
    Flux<ICombatSnapshot> observe(String machineFullName);

    /**
     * Clears skill cooldown bookkeeping and in-flight cast flags for the machine.
     */
    void resetCooldowns(String machineFullName);
}

package org.sokybot.combat.api;

import java.util.concurrent.CompletableFuture;

import org.sokybot.engine.api.workflow.IWorkflowContext;

/**
 * SPI: async weapon loadout switching with server-ack semantics (similar to NPC interaction facades).
 */
public interface IWeaponSwapCoordinator {

    /**
     * Best-known loadout for this machine; {@link WeaponLoadout#UNKNOWN} until first swap or observation.
     */
    WeaponLoadout currentLoadout(String machineFullName);

    /**
     * Moves inventory items into equip slots for the requested loadout. Completes when the server acknowledges
     * the equip operation(s), or completes exceptionally on timeout / error.
     */
    CompletableFuture<Void> swapTo(String machineFullName, WeaponLoadout target);

    /**
     * Preferred when a workflow context is available (dispatcher + machine-scoped settings snapshot).
     */
    CompletableFuture<Void> swapTo(IWorkflowContext ctx, WeaponLoadout target);
}

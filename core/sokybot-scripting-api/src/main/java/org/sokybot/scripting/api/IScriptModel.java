package org.sokybot.scripting.api;

import java.util.Collection;
import java.util.Optional;

import org.sokybot.navigation.api.WorldPoint;

import reactor.core.publisher.Flux;

/**
 * OSGi-facing model: script catalogue plus per-machine reactive execution overlay.
 */
public interface IScriptModel {

    Optional<ITravelScript> findById(String id);

    Collection<ITravelScript> listAvailable();

    Optional<IScriptExecutionSnapshot> snapshot(String machineId);

    Flux<IScriptExecutionSnapshot> observe(String machineId);

    /** Drops cursor and clears in-flight phases for the machine (death / relog). */
    void reset(String machineId);

    // --- Executor coordination (defaults preserve compatibility; overridden by scripting-core) ---

    /** Bind which script id tracks execution state for this machine. */
    default void bindActiveScript(String machineId, String scriptId) {
        bindActiveScript(machineId, scriptId, 1.5f);
    }

    /** Bind script id and movement arrival tolerance for walk/portal completion. */
    default void bindActiveScript(String machineId, String scriptId, float arrivalToleranceWorldUnits) {
    }

    /** Executor issued NPC teleport handshake packets for the current TELEPORT step. */
    default void onTeleportStepIssued(String machineId) {
    }

    /** Executor began surface movement toward {@code target} for WALK or PORTAL steps. */
    default void onWalkProbeBound(String machineId, WorldPoint target, boolean portalArrival) {
    }

    /** Executor entered a scripted WAIT sleep window. */
    default void onWaitScheduled(String machineId, long waitMillis) {
    }

    /** Executor finished a LOG line locally (advance projection cursor). */
    default void onLogLineExecuted(String machineId) {
    }
}

package org.sokybot.scripting.core.internal;

import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.scripting.api.IScriptExecutionSnapshot;
import org.sokybot.scripting.api.ScriptPhase;

import reactor.core.publisher.Sinks;

/**
 * Mutable per-machine projection row for scripted travel.
 */
final class MachineScriptState {

    volatile String scriptId;
    volatile int cursor;
    volatile ScriptPhase phase = ScriptPhase.IDLE;
    volatile long phaseStartedAtEpochMs = System.currentTimeMillis();
    volatile long teleportRequestedAtEpochMs;
    volatile String lastError;
    volatile WorldPoint walkTarget;
    volatile boolean portalStyleArrival;
    volatile float arrivalToleranceWorldUnits = 1.5f;
    volatile long sleepUntilEpochMs;
    volatile Integer selfEntityId;

    final Sinks.Many<IScriptExecutionSnapshot> sink = Sinks.many().multicast().onBackpressureBuffer(64, false);
}

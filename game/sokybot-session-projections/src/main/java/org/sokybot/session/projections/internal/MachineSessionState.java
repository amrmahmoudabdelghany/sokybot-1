package org.sokybot.session.projections.internal;

import java.util.Optional;

import org.sokybot.session.api.CaptchaPrompt;
import org.sokybot.session.api.SessionPhase;

import reactor.core.publisher.Sinks;
import org.sokybot.session.api.ISessionSnapshot;

/**
 * Mutable per-machine session state accumulator (projection-owned).
 * <p>
 * Not exported; only used internally by {@link SessionModelComponent}.
 */
final class MachineSessionState {

    volatile SessionPhase phase = SessionPhase.OFFLINE;
    volatile long phaseStartedAtEpochMs;
    volatile int reconnectAttempt;
    volatile long nextAttemptAtEpochMs;
    volatile String lastDisconnectReason;
    volatile CaptchaPrompt pendingCaptcha;

    final Sinks.Many<ISessionSnapshot> snapshotSink =
            Sinks.many().multicast().onBackpressureBuffer(64, false);

    void transitionTo(SessionPhase newPhase) {
        this.phase = newPhase;
        this.phaseStartedAtEpochMs = System.currentTimeMillis();
    }

    void resetOnConnect() {
        this.reconnectAttempt = 0;
        this.nextAttemptAtEpochMs = 0L;
        this.lastDisconnectReason = null;
        this.pendingCaptcha = null;
    }

    Optional<CaptchaPrompt> pendingCaptchaOpt() {
        return Optional.ofNullable(pendingCaptcha);
    }
}

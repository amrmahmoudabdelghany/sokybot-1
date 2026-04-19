package org.sokybot.session.projections.internal;

import java.util.Optional;

import org.sokybot.session.api.CaptchaPrompt;
import org.sokybot.session.api.ISessionSnapshot;
import org.sokybot.session.api.SessionPhase;

/**
 * Immutable snapshot of a machine's session state at a point in time.
 */
final class SessionSnapshotImpl implements ISessionSnapshot {

    private final String machineId;
    private final SessionPhase phase;
    private final long phaseStartedAtEpochMs;
    private final int reconnectAttempt;
    private final long nextAttemptAtEpochMs;
    private final String lastDisconnectReason;
    private final CaptchaPrompt pendingCaptcha;

    SessionSnapshotImpl(String machineId, MachineSessionState state) {
        this.machineId = machineId;
        this.phase = state.phase;
        this.phaseStartedAtEpochMs = state.phaseStartedAtEpochMs;
        this.reconnectAttempt = state.reconnectAttempt;
        this.nextAttemptAtEpochMs = state.nextAttemptAtEpochMs;
        this.lastDisconnectReason = state.lastDisconnectReason;
        this.pendingCaptcha = state.pendingCaptcha;
    }

    @Override
    public String getMachineId() {
        return machineId;
    }

    @Override
    public SessionPhase getPhase() {
        return phase;
    }

    @Override
    public long getPhaseStartedAtEpochMs() {
        return phaseStartedAtEpochMs;
    }

    @Override
    public int getReconnectAttempt() {
        return reconnectAttempt;
    }

    @Override
    public long getNextAttemptAtEpochMs() {
        return nextAttemptAtEpochMs;
    }

    @Override
    public String getLastDisconnectReason() {
        return lastDisconnectReason;
    }

    @Override
    public Optional<CaptchaPrompt> getPendingCaptcha() {
        return Optional.ofNullable(pendingCaptcha);
    }

    @Override
    public String toString() {
        return "SessionSnapshot{machine='" + machineId
                + "', phase=" + phase
                + ", reconnectAttempt=" + reconnectAttempt + '}';
    }
}

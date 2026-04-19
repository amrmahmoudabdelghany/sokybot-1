package org.sokybot.session.api;

import java.util.Optional;

/**
 * Read-only snapshot of a machine's session state at a point in time.
 * <p>
 * Implementations are created by the session projections bundle.
 * Consumers should treat instances as immutable.
 */
public interface ISessionSnapshot {

    /**
     * Gets the machine identifier this snapshot belongs to.
     */
    String getMachineId();

    /**
     * Gets the current session phase.
     */
    SessionPhase getPhase();

    /**
     * Gets the epoch millis when the current phase was entered.
     */
    long getPhaseStartedAtEpochMs();

    /**
     * Gets the 1-based reconnect attempt counter.
     * Resets to 0 on successful {@link SessionPhase#CONNECTED}.
     */
    int getReconnectAttempt();

    /**
     * Gets the epoch millis of the next scheduled reconnection attempt.
     * Returns 0 when not in {@link SessionPhase#RECONNECTING}.
     */
    long getNextAttemptAtEpochMs();

    /**
     * Gets the reason for the last disconnect (nullable).
     */
    String getLastDisconnectReason();

    /**
     * Gets the pending captcha prompt, if any.
     */
    Optional<CaptchaPrompt> getPendingCaptcha();
}

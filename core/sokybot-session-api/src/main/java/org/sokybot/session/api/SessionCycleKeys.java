package org.sokybot.session.api;

/**
 * Shared constant keys for session-related state in the login-cycle blackboard.
 * <p>
 * These keys allow cross-referencing between the login-cycle's persistent data
 * and the session projection model.
 */
public final class SessionCycleKeys {

    private SessionCycleKeys() {
        // utility class — no instantiation
    }

    /** Epoch millis of the last disconnect event. */
    public static final String KEY_LAST_DISCONNECT_AT_MS = "session.lastDisconnectAtMs";

    /** 1-based ID of the most recent reconnect attempt. */
    public static final String KEY_LAST_RECONNECT_ATTEMPT_ID = "session.lastReconnectAttemptId";

    /** Epoch millis when the session last reached CONNECTED. */
    public static final String KEY_LAST_CONNECTED_AT_MS = "session.lastConnectedAtMs";

    /** Epoch millis since a captcha challenge has been pending. */
    public static final String KEY_CAPTCHA_PENDING_SINCE_MS = "session.captchaPendingSinceMs";
}

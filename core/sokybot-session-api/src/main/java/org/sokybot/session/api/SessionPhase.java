package org.sokybot.session.api;

/**
 * Represents the lifecycle phase of a bot session.
 * <p>
 * Phases are ordered from initial offline state through connection,
 * authentication, and potential reconnection cycles.
 */
public enum SessionPhase {

    /** No proxy session ever started for this machine. */
    OFFLINE,

    /** {@code connectToServer(...)} issued, no {@code onServerConnected} yet. */
    CONNECTING,

    /** Server connected, security/handshake in progress. */
    HANDSHAKING,

    /** Between {@code onHandshakeComplete} and {@code onAuthenticated}. */
    AUTHENTICATING,

    /** Server requested captcha/passcode and the active solver chose to stall. */
    CAPTCHA_PENDING,

    /** {@code onAuthenticated} received and channel still alive. */
    CONNECTED,

    /** {@code onDisconnected(...)} received; reconnect not yet started. */
    DISCONNECTED,

    /** Disconnect happened and the login-cycle is actively retrying. */
    RECONNECTING
}

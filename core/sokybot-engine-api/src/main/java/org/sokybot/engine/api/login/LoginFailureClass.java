package org.sokybot.engine.api.login;

/**
 * Coarse classes for login failure handling and retry policy decisions.
 */
public enum LoginFailureClass {
    NETWORK,
    AGENT_TIMEOUT,
    CREDENTIAL,
    GHOST_COOLDOWN,
    MANUAL_VERIFICATION,
    CHARACTER_NOT_FOUND,
    SERVER_INSPECTION,
    MISSING_PREREQ,
    UNKNOWN_RETRY,
    FATAL
}

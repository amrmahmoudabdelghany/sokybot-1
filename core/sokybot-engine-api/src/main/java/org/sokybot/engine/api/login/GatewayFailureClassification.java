package org.sokybot.engine.api.login;

/**
 * Stable failure buckets aligned with {@code FAILURE_*} strings in Login.groovy.
 */
public enum GatewayFailureClassification {

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

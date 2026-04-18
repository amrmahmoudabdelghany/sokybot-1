package org.sokybot.engine.api.login;

/**
 * Result returned by interactive login coordinator handlers.
 */
public enum InteractiveOutcome {
    NONE,
    WAITING_FOR_PASSCODE,
    WAITING_FOR_CAPTCHA,
    WAITING_FOR_USER_RESUME,
    IN_QUEUE,
    TIMED_OUT_DISCONNECT
}

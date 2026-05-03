package org.sokybot.behaviors.logistics.quartermaster;

/**
 * Vault mutex lifecycle for a {@link LockState} keyed by storage session id.
 */
enum LockStatus {

    IDLE,
    GRANTED,
    SORTING
}

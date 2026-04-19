package org.sokybot.pet.api;

/**
 * String constants reserved for future pet-cycle workflow wiring (additive).
 */
public final class PetCycleKeys {

    private PetCycleKeys() {
    }

    /** Registered cycle id placeholder for a dedicated pet workflow (not used yet). */
    public static final String CYCLE_NAME = "pet-cycle";

    /** Blackboard: last pet feed timestamp (epoch ms). */
    public static final String KEY_LAST_FEED_AT_MS = "pet.lastFeedAtEpochMs";

    /** Blackboard: catalogue ref id of item last used to feed (if applicable). */
    public static final String KEY_LAST_FEED_REF_ID = "pet.lastFeedItemRefId";
}

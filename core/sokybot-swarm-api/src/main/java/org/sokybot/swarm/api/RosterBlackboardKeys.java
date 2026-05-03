package org.sokybot.swarm.api;

/**
 * Persistent blackboard keys for Epic #17 roster rally / leader invite handshake.
 */
public final class RosterBlackboardKeys {

    private RosterBlackboardKeys() {
    }

    /**
     * Recruit workflow: rally destination ({@link org.sokybot.navigation.api.WorldPoint}) while responding to
     * {@link SwarmDispatchEvent}.
     */
    public static final String KEY_ROSTER_RALLY_DESTINATION = "roster.recruit.rallyDestination";

    /**
     * Leader workflow: pending recruit character names to invite when in range (typically a
     * {@link java.util.concurrent.ConcurrentHashMap#newKeySet() concurrent set}).
     */
    public static final String KEY_PENDING_ROSTER_INVITES = "roster.leader.pendingInviteCharNames";
}

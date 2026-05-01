package org.sokybot.swarm.api;

/**
 * Persistent blackboard keys for lure FSM state on {@link org.sokybot.engine.api.workflow.IWorkflowContext#getPersistentData()}.
 */
public final class SwarmLureBlackboardKeys {

    private SwarmLureBlackboardKeys() {
    }

    public static final String KEY_LURE_REQUEST_ID = "swarm.lure.requestId";
    public static final String KEY_LURE_PHASE = "swarm.lure.phase";
    public static final String KEY_SYNC_EPOCH_MS = "swarm.lure.syncEpochMs";
    public static final String KEY_AGREED_TTT_MS = "swarm.lure.agreedTimeToTargetMs";
    public static final String KEY_SLICE_INDEX = "swarm.lure.sliceIndex";

    /** Number of assigned lurers ({@code assignedLurers.size()} at capture time). */
    public static final String KEY_LURER_COUNT = "swarm.lure.lurerCount";

    /** Local completion of fan-out leg (within arrival radius of fan waypoint). */
    public static final String KEY_FAN_OUT_COMPLETE = "swarm.lure.fanOutComplete";

    /** Fan-out waypoint near anchor (world coordinates). */
    public static final String KEY_FAN_X = "swarm.lure.fanX";
    public static final String KEY_FAN_Y = "swarm.lure.fanY";
    public static final String KEY_FAN_Z = "swarm.lure.fanZ";

    /**
     * Optional packed fan waypoint payload (e.g. JSON); numeric components may use {@link #KEY_FAN_X}, {@link #KEY_FAN_Y},
     * {@link #KEY_FAN_Z}.
     */
    public static final String KEY_FAN_XYZ = "swarm.lure.fan.xyz";

    public static final String KEY_ANCHOR_MACHINE_ID = "swarm.lure.anchorMachineId";
    public static final String KEY_ANCHOR_X = "swarm.lure.anchorX";
    public static final String KEY_ANCHOR_Y = "swarm.lure.anchorY";
    public static final String KEY_ANCHOR_Z = "swarm.lure.anchorZ";

    public static final String KEY_FAN_RADIUS_WORLD = "swarm.lure.fanRadiusWorld";
    public static final String KEY_CONVERGE_ARMED = "swarm.lure.convergeArmed";
    public static final String KEY_LAST_NAV_ISSUED_AT_MS = "swarm.lure.lastNavIssuedAtEpochMs";

    /** When true, anchor-side combat should suppress ordinary engage (Epic #14). */
    public static final String KEY_ANCHOR_SUPPRESS_ENGAGE = "swarm.lure.anchorSuppressEngage";

    /** Anchor FSM: wall-clock ms when PULLING phase was first observed for the active request. */
    public static final String KEY_ANCHOR_PULL_START_EPOCH_MS = "swarm.lure.anchor.pullStartEpochMs";

    /** Anchor FSM: {@link Boolean#TRUE} after this anchor published CONVERGING for the active request. */
    public static final String KEY_ANCHOR_CONVERGE_PUBLISHED = "swarm.lure.anchor.convergePublished";

    /** Anchor FSM: {@link SwarmEvent#getRequestId()} currently driving local anchor state. */
    public static final String KEY_ANCHOR_ACTIVE_REQUEST_ID = "swarm.lure.anchor.activeRequestId";

    /** Anchor FSM: published converge instant (epoch ms) retained for the nuke gate. */
    public static final String KEY_ANCHOR_PUBLISHED_SYNC_EPOCH_MS = "swarm.lure.anchor.publishedSyncEpochMs";

    /** Anchor FSM: {@link Boolean#TRUE} after nuke cast issued for {@link #KEY_ANCHOR_ACTIVE_REQUEST_ID}. */
    public static final String KEY_ANCHOR_NUKE_FIRED = "swarm.lure.anchor.nukeFired";
}

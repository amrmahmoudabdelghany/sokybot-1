package org.sokybot.behaviors.logistics;

/**
 * Keys for {@link org.sokybot.engine.api.workflow.IWorkflowContext#getPersistentData()} in the logistics swarm cycle.
 */
public final class LogisticsCycleKeys {

    /** Registered workflow cycle id (see {@link LogisticsActuator}). */
    public static final String CYCLE_NAME = "logistics-cycle";

    public static final String KEY_PENDING_REQUEST_ID = "logistics_pending_request_id";
    public static final String KEY_PENDING_REQUEST_AT_MS = "logistics_pending_request_at_ms";
    public static final String KEY_CLAIMED_BY_MULE_ID = "logistics_claimed_by_mule_id";
    public static final String KEY_EXPECTED_MULE_TRAINER_UID = "logistics_expected_mule_trainer_uid";
    public static final String KEY_ACTIVE_SWARM_REQUEST_ID = "logistics_active_swarm_request_id";

    /** Mule navigation / exchange (shared with swarm behaviors bundle). */
    public static final String SWARM_TARGET_X = "swarm_target_x";
    public static final String SWARM_TARGET_Z = "swarm_target_z";
    public static final String SWARM_TARGET_Y = "swarm_target_y";
    public static final String SWARM_FARMER_SPAWN_ID = "swarm_farmer_spawn_id";
    public static final String SWARM_FARMER_MACHINE_ID = "swarm_farmer_machine_id";
    public static final String SWARM_ARRIVED = "swarm_arrived";
    public static final String SWARM_JOB_STARTED_MS = "swarm_job_started_ms";
    public static final String SWARM_EXCHANGE_ATTEMPTS = "swarm_exchange_attempts";
    public static final String SWARM_JOB_MONITOR_REGISTERED = "swarm_job_monitor_registered";
    public static final String SWARM_MULE_APPROVE_SENT = "swarm_mule_approve_sent";
    public static final String SWARM_MULE_FINALIZE_SENT = "swarm_mule_finalize_sent";
    public static final String KEY_ROUTE_CURSOR = "swarm_route_cursor";
    public static final String KEY_ROUTE_GENERATION = "swarm_route_generation";
    public static final String KEY_ROUTE_HOP_COUNT = "swarm_route_hop_count";

    private LogisticsCycleKeys() {
    }
}

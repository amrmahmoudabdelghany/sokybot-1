package org.sokybot.swarm.api;

/**
 * Epic #18 Caravan Syndicate: workflow persistent keys for telemetry-driven escort.
 */
public final class CaravanBlackboardKeys {

    /**
     * Value type: {@link SwarmCaravanTelemetryEvent} — latest Trader telemetry for this Hunter session.
     */
    public static final String KEY_LATEST_CARAVAN_TELEMETRY = "caravan_latest_telemetry";

    /** Value type: {@link org.sokybot.navigation.api.WorldPoint} — previous Trader position for velocity (Trader heartbeat). */
    public static final String KEY_CARAVAN_LAST_POS = "caravan_last_pos";

    /** Value type: {@link Long} — epoch millis when {@link #KEY_CARAVAN_LAST_POS} was recorded. */
    public static final String KEY_CARAVAN_LAST_TIME = "caravan_last_time_ms";

    /** Value type: {@link Long} — epoch millis of last Hunter formation move (escort throttle). */
    public static final String KEY_CARAVAN_LAST_MOVE_TIME = "caravan_last_move_time_ms";

    /** Value type: {@link Integer} — catalogue ref id of the attacker to defend against (Hunter threat collapse). */
    public static final String KEY_THREAT_REF_ID = "caravan_threat_ref_id";

    /** Value type: {@link org.sokybot.navigation.api.WorldPoint} — approximate attacker position from Trader telemetry. */
    public static final String KEY_THREAT_POS = "caravan_threat_pos";

    /** Value type: {@link Long} — epoch millis until threat defend window expires. */
    public static final String KEY_DEFEND_UNTIL_MS = "caravan_defend_until_ms";

    private CaravanBlackboardKeys() {
    }
}

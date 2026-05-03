package org.sokybot.swarm.api;

import java.util.Map;

/**
 * Persistent workflow keys for Treasury Mesh (Epic #19).
 */
public final class TreasuryMeshBlackboardKeys {

    /**
     * Value type: {@link java.util.Map} with {@link Integer} item ref id → epoch millis of last distress publish.
     */
    public static final String KEY_LAST_DISTRESS_TIMES = "treasury_mesh_last_distress_times";

    /** Value type: {@link String}. */
    public static final String KEY_TREASURY_TRADE_SESSION_ID = "treasury_mesh_trade_session_id";

    /** Value type: {@link org.sokybot.navigation.api.WorldPoint}. */
    public static final String KEY_TREASURY_RENDEZVOUS_WP = "treasury_mesh_rendezvous_wp";

    /** Value type: {@link String} (peer machine full name). */
    public static final String KEY_TREASURY_PEER_MACHINE_ID = "treasury_mesh_peer_machine_id";

    /** Value type: {@link TreasuryRole}. */
    public static final String KEY_TREASURY_ROLE = "treasury_mesh_role";

    /** Value type: {@link TreasuryPhase}. */
    public static final String KEY_TREASURY_PHASE = "treasury_mesh_phase";

    /** Value type: {@link Integer}. */
    public static final String KEY_TREASURY_ITEM_REF_ID = "treasury_mesh_item_ref_id";

    /** Value type: {@link Integer}. */
    public static final String KEY_TREASURY_COMMITTED_AMOUNT = "treasury_mesh_committed_amount";

    /** Value type: {@link Long} (epoch millis, rendezvous / trade window deadline). */
    public static final String KEY_TREASURY_DEADLINE_MS = "treasury_mesh_deadline_ms";

    private TreasuryMeshBlackboardKeys() {
    }

    /**
     * Removes all Treasury Mesh session keys from workflow persistent data.
     */
    public static void clearTreasurySession(Map<String, Object> persistentData) {
        if (persistentData == null) {
            return;
        }
        persistentData.remove(KEY_TREASURY_TRADE_SESSION_ID);
        persistentData.remove(KEY_TREASURY_RENDEZVOUS_WP);
        persistentData.remove(KEY_TREASURY_PEER_MACHINE_ID);
        persistentData.remove(KEY_TREASURY_ROLE);
        persistentData.remove(KEY_TREASURY_PHASE);
        persistentData.remove(KEY_TREASURY_ITEM_REF_ID);
        persistentData.remove(KEY_TREASURY_COMMITTED_AMOUNT);
        persistentData.remove(KEY_TREASURY_DEADLINE_MS);
    }
}

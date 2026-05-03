package org.sokybot.swarm.api.quartermaster;

/**
 * Epic #15: {@link org.sokybot.engine.api.workflow.IWorkflowContext#getPersistentData()} keys for storage lock, dump phase, and QM sort trigger.
 */
public final class QuartermasterBlackboardKeys {

    private QuartermasterBlackboardKeys() {
    }

    /** Value type: {@link String} — token from {@link SwarmStorageLockGrantedEvent}. */
    public static final String KEY_STORAGE_LOCK_TOKEN = "quartermaster.storageLockToken";

    /** Value type: {@link String} — farmer dump state machine phase label. */
    public static final String KEY_STORAGE_DUMP_PHASE = "quartermaster.storageDumpPhase";

    /** Value type: {@link Boolean} — signal that QM should run sort after release. */
    public static final String KEY_QUARTERMASTER_SORT_NEEDED = "quartermaster.sortNeeded";
}

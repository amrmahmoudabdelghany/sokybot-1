package org.sokybot.swarm.api.symphony;

/**
 * Epic #21: {@link org.sokybot.engine.api.workflow.IWorkflowContext#getPersistentData()} keys for symphony hold state.
 */
public final class SymphonyBlackboardKeys {

    private SymphonyBlackboardKeys() {
    }

    /** Value type: {@link Long} — epoch ms until normal engage behaviors may resume. */
    public static final String KEY_SYMPHONY_HOLD_UNTIL_MS = "symphony.holdUntilMs";

    /** Value type: {@link String} — active {@link ComboDefinition#getComboId()}. */
    public static final String KEY_SYMPHONY_ACTIVE_COMBO_ID = "symphony.activeComboId";

    /** Value type: {@link Integer} — game ref id for the combo target. */
    public static final String KEY_SYMPHONY_TARGET_REF_ID = "symphony.targetRefId";

    /** Value type: {@link Integer} — payload skill ref to fire after effect lands. */
    public static final String KEY_SYMPHONY_PAYLOAD_SKILL_REF_ID = "symphony.payloadSkillRefId";
}

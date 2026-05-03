package org.sokybot.swarm.api.shapeshifter;

/**
 * Epic #22: {@link org.sokybot.engine.api.workflow.IWorkflowContext#getPersistentData()} keys for flex state.
 */
public final class ShapeshifterBlackboardKeys {

    private ShapeshifterBlackboardKeys() {
    }

    /** Value type: {@link Boolean}. */
    public static final String KEY_SHAPESHIFTER_FLEX_ACTIVE = "shapeshifter.flexActive";

    /** Value type: {@link String} — {@link org.sokybot.party.api.shapeshifter.SwarmTacticalRole} name. */
    public static final String KEY_SHAPESHIFTER_ACTIVE_ROLE = "shapeshifter.activeRole";

    /** Value type: {@link String} — baseline workflow cycle id before flex. */
    public static final String KEY_SHAPESHIFTER_BASELINE_CYCLE_ID = "shapeshifter.baselineCycleId";

    /** Value type: {@link Integer} — weapon item ref before flex. */
    public static final String KEY_SHAPESHIFTER_BASELINE_WEAPON_REF = "shapeshifter.baselineWeaponRef";
}

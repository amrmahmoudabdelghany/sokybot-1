package org.sokybot.combat.api;

/**
 * String constants for the combat-cycle workflow: cycle id, behavior ids, blackboard keys, and phase labels.
 */
public final class CombatCycleKeys {

    private CombatCycleKeys() {
    }

    /** Registered cycle id passed to {@code BehaviorCycleSpec}. */
    public static final String CYCLE_NAME = "combat-cycle";

    /** Behavior ids (normalized names for assembler CHECK_/WAIT_AFTER_ states). */
    public static final String BEHAVIOR_RECOVER = "recover";
    public static final String BEHAVIOR_LOOT = "loot";
    public static final String BEHAVIOR_SEARCH = "search";
    public static final String BEHAVIOR_ENGAGE = "engage";
    public static final String BEHAVIOR_MAINTAIN_BUFFS = "maintainBuffs";
    public static final String BEHAVIOR_ENSURE_IMBUE = "ensureImbue";
    public static final String BEHAVIOR_UNSTUCK = "unstuck";

    /** Blackboard keys on {@link org.sokybot.engine.api.workflow.IWorkflowContext#getPersistentData()}. */
    public static final String KEY_CURRENT_TARGET_ENTITY_ID = "combat.currentTargetEntityId";
    public static final String KEY_LAST_SKILL_EXECUTED_AT_MS = "combat.lastSkillExecutedAtMs";
    public static final String KEY_LEASH_ANCHOR_X = "combat.leashAnchorX";
    public static final String KEY_LEASH_ANCHOR_Y = "combat.leashAnchorY";
    public static final String KEY_LEASH_ANCHOR_Z = "combat.leashAnchorZ";
    public static final String KEY_LOOT_DEADLINE_MS = "combat.lootDeadlineMs";
    public static final String KEY_RECOVERY_LAST_ACTION_AT_MS = "combat.recoveryLastActionAtMs";
    public static final String KEY_SKILL_IN_FLIGHT = "combat.skillInFlight";
    public static final String KEY_COMBAT_PHASE = "combat.phase";
    public static final String KEY_IMBUE_LAST_SKILL_CAST_AT_MS = "combat.imbue.lastSkillCastAtMs";
    public static final String KEY_IMBUE_ATTACKS_SINCE_LAST_CAST = "combat.imbue.attacksSinceLastCast";
    public static final String KEY_WEAPON_CURRENT_LOADOUT = "combat.weapon.currentLoadout";
    public static final String KEY_WEAPON_PREVIOUS_LOADOUT = "combat.weapon.previousLoadout";

    /** Human-oriented phase strings for telemetry / UI. */
    public static final String PHASE_IDLE = "IDLE";
    public static final String PHASE_HUNTING = "HUNTING";
    public static final String PHASE_ENGAGED = "ENGAGED";
    public static final String PHASE_LOOTING = "LOOTING";
    public static final String PHASE_RECOVERING = "RECOVERING";
    public static final String PHASE_LEASH_RETURN = "LEASH_RETURN";
    public static final String PHASE_UNSTUCK = "UNSTUCK";
}

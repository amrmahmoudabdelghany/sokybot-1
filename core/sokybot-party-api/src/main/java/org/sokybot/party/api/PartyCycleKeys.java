package org.sokybot.party.api;

/**
 * Cycle name, behavior ids, and blackboard keys for party automation.
 */
public final class PartyCycleKeys {

    private PartyCycleKeys() {
    }

    /** Registered cycle id for {@code BehaviorCycleSpec}. */
    public static final String CYCLE_NAME = "party-cycle";

    public static final String BEHAVIOR_AUTO_ACCEPT = "partyAutoAccept";
    public static final String BEHAVIOR_AUTO_INVITE = "partyAutoInvite";
    public static final String BEHAVIOR_HEAL_PARTY = "partyHeal";
    public static final String BEHAVIOR_BUFF_PARTY = "partyBuff";
    public static final String BEHAVIOR_FOLLOW_LEADER = "partyFollowLeader";

    /** Epic #17: recruit walks to swarm rally point during party cycle. */
    public static final String BEHAVIOR_ROSTER_RALLY_PARTY = "party.rosterRally";

    /** Epic #17: leader auto-invite when recruit is in matrix proximity. */
    public static final String BEHAVIOR_ROSTER_LEADER_INVITE = "party.rosterLeaderInvite";

    /**
     * Blackboard keys on {@link org.sokybot.engine.api.workflow.IWorkflowContext#getPersistentData()}.
     */
    public static final String KEY_LAST_INVITE_DECISION_AT_MS = "party.lastInviteDecisionAtEpochMs";
    public static final String KEY_LAST_PARTY_HEAL_AT_MS = "party.lastPartyHealAtEpochMs";
    public static final String KEY_LAST_PARTY_FOLLOW_AT_MS = "party.lastPartyFollowAtEpochMs";
    public static final String KEY_LAST_PARTY_BUFF_AT_MS = "party.lastPartyBuffAtEpochMs";
    public static final String KEY_BUFF_SKILL_INDEX = "party.buffSkillRoundRobinIndex";
    public static final String KEY_AUTO_INVITE_PREFIX = "party.autoInvite.";
}

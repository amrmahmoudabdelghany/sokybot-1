package org.sokybot.party.api;

import java.util.Collections;
import java.util.List;

/**
 * Derived policy thresholds for party automation strategies (whitelist, follow, heal, buff share).
 */
public interface IPartyPolicy {

    /** When false, the dedicated party-cycle actuator does not attach (behaviors only run if explicitly enabled). */
    default boolean isPartyCycleEnabled() {
        return false;
    }

    default boolean isAutoAcceptInvites() {
        return false;
    }

    /** Character names allowed to auto-accept invites from; empty allows none unless policy widened later. */
    default List<String> getPartyInviteWhitelist() {
        return Collections.emptyList();
    }

    /** When true, behaviors may create or advertise a matching party entry (server permitting). */
    default boolean isAutoCreateMatch() {
        return false;
    }

    /** When true, invite candidates from the matching list whose title matches {@link #getMatchingTitle()}. */
    default boolean isAutoInviteFromMatching() {
        return false;
    }

    /** Title string used when posting to party matching (game-specific conventions). */
    default String getMatchingTitle() {
        return "";
    }

    default boolean isHealOthers() {
        return false;
    }

    /** Cast heal support when target member HP is at or below this percent (0–100). */
    default int getHealHpThresholdPercent() {
        return 50;
    }

    /** Skill ref id for party heal casts; non-positive disables heal automation. */
    default int getHealSkillRefId() {
        return -1;
    }

    default boolean isFollowLeader() {
        return false;
    }

    /** World-space distance within which follow behavior does not issue movement. */
    default float getFollowDistanceWorld() {
        return 25.0f;
    }

    /** Skill ref ids considered for party buff sharing / refresh rotation. */
    default List<Integer> getBuffShareSkillIds() {
        return Collections.emptyList();
    }
}

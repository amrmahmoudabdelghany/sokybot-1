package org.sokybot.party.api;

import java.util.Collections;
import java.util.List;

/**
 * Read-only persisted party automation settings mirrored from machine-scoped user configuration.
 */
public interface IPartySettings {

    default boolean isPartyCycleEnabled() {
        return false;
    }

    default boolean isAutoAcceptInvites() {
        return false;
    }

    default List<String> getPartyInviteWhitelist() {
        return Collections.emptyList();
    }

    default boolean isAutoCreateMatch() {
        return false;
    }

    default boolean isAutoInviteFromMatching() {
        return false;
    }

    default String getMatchingTitle() {
        return "";
    }

    default boolean isHealOthers() {
        return false;
    }

    default int getHealHpThresholdPercent() {
        return 50;
    }

    default int getHealSkillRefId() {
        return -1;
    }

    default boolean isFollowLeader() {
        return false;
    }

    default float getFollowDistanceWorld() {
        return 25.0f;
    }

    default List<Integer> getBuffShareSkillIds() {
        return Collections.emptyList();
    }

    default String getLeaderMachineFullName() {
        return "";
    }

    /**
     * Derives an {@link IPartyPolicy} from these settings for strategy calls.
     */
    IPartyPolicy toPolicy();
}

package org.sokybot.combat.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable point-in-time tactical view of combat-relevant state for one machine.
 */
public interface ICombatSnapshot {

    String getMachineFullName();

    long getSnapshotEpochMs();

    /** Local player entity id when known; empty if unknown. */
    Optional<Integer> getSelfEntityId();

    Optional<Integer> getCurrentTargetEntityId();

    int getCurrentHp();

    int getMaxHp();

    int getCurrentMp();

    int getMaxMp();

    float getSelfX();

    float getSelfY();

    float getSelfZ();

    List<MonsterRef> getNearbyMonsters();

    List<DroppedItemRef> getNearbyLoot();

    /**
     * Skill reference id -> epoch millis when the skill may be executed again (cooldown end).
     * Missing entries mean unknown or off cooldown.
     */
    Map<Integer, Long> getSkillCooldownReadyAtEpochMs();

    /** Non-empty while a cast is in flight until confirm/end/error. */
    boolean isSkillCastInFlight();

    /** Berserk / burst window end time; empty if not active. */
    Optional<Long> getBerserkActiveUntilEpochMs();
}

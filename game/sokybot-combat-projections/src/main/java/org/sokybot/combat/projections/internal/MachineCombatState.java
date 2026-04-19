package org.sokybot.combat.projections.internal;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.sokybot.combat.api.ActiveBuff;

/**
 * Mutable per-machine tactical accumulator (projection-owned; not part of {@link org.sokybot.gamemodel.IGameModel}).
 */
final class MachineCombatState {

    volatile Integer selfEntityId;
    volatile Integer currentTargetEntityId;

    volatile float selfX;
    volatile float selfY;
    volatile float selfZ;

    volatile int currentHp;
    volatile int maxHp = 1;
    volatile int currentMp;
    volatile int maxMp = 1;

    final ConcurrentMap<Integer, TacticalMonster> monsters = new ConcurrentHashMap<>();
    final ConcurrentMap<Integer, TacticalLoot> loot = new ConcurrentHashMap<>();

    /** Skill ref id -> epoch millis when ready. */
    final ConcurrentMap<Integer, Long> skillCooldownReadyAtEpochMs = new ConcurrentHashMap<>();

    /** Storage key is {@code buffId} when non-zero, else {@code skillRefId}. */
    final ConcurrentMap<Integer, ActiveBuff> activeBuffsById = new ConcurrentHashMap<>();

    volatile boolean skillCastInFlight;
    volatile Long berserkActiveUntilEpochMs;

    void resetCooldowns() {
        skillCooldownReadyAtEpochMs.clear();
        skillCastInFlight = false;
    }

    Optional<Integer> selfOrEmpty() {
        return Optional.ofNullable(selfEntityId);
    }

}

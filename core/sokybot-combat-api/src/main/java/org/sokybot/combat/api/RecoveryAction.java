package org.sokybot.combat.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Recovery step (potions, scrolls, etc.).
 */
public final class RecoveryAction {

    private final RecoveryActionKind kind;
    /** Game item reference id when {@link RecoveryActionKind#ITEM_USE} or consumable use. */
    private final int itemRefId;
    /** Millis before this action should be attempted again (throttle). */
    private final Long cooldownUntilEpochMs;

    public RecoveryAction(RecoveryActionKind kind, int itemRefId, Long cooldownUntilEpochMs) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.itemRefId = itemRefId;
        this.cooldownUntilEpochMs = cooldownUntilEpochMs;
    }

    public RecoveryActionKind getKind() {
        return kind;
    }

    public int getItemRefId() {
        return itemRefId;
    }

    public Optional<Long> getCooldownUntilEpochMs() {
        return Optional.ofNullable(cooldownUntilEpochMs);
    }
}

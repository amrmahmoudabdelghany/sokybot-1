package org.sokybot.combat.api;

import java.util.Objects;

/**
 * A ground drop the bot may attempt to loot.
 */
public final class DroppedItemRef {

    private final int entityId;
    private final int itemRefId;
    private final Integer ownerEntityId;
    private final long ownerExpiresAtEpochMs;
    private final float distanceToSelf;

    public DroppedItemRef(int entityId, int itemRefId, Integer ownerEntityId,
            long ownerExpiresAtEpochMs, float distanceToSelf) {
        this.entityId = entityId;
        this.itemRefId = itemRefId;
        this.ownerEntityId = ownerEntityId;
        this.ownerExpiresAtEpochMs = ownerExpiresAtEpochMs;
        this.distanceToSelf = distanceToSelf;
    }

    public int getEntityId() {
        return entityId;
    }

    public int getItemRefId() {
        return itemRefId;
    }

    /** Null when the drop is free-for-all. */
    public Integer getOwnerEntityId() {
        return ownerEntityId;
    }

    /** After this instant the drop may become lootable by anyone (game-dependent). */
    public long getOwnerExpiresAtEpochMs() {
        return ownerExpiresAtEpochMs;
    }

    public float getDistanceToSelf() {
        return distanceToSelf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DroppedItemRef that = (DroppedItemRef) o;
        return entityId == that.entityId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId);
    }
}

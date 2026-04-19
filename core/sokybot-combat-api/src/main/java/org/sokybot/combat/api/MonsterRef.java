package org.sokybot.combat.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Lightweight view of a nearby monster candidate for targeting.
 */
public final class MonsterRef {

    private final int entityId;
    private final int refObjId;
    private final int levelOrZero;
    private final float distanceToSelf;
    private final int hpPercentOrNegativeIfUnknown;
    private final boolean aggressiveTowardSelf;
    private final boolean championOrUnique;
    private final Optional<Integer> firstAttackerEntityId;
    private final Optional<Float> distanceFromTrainingAnchor;
    private final MonsterTier tier;

    /**
     * Legacy constructor; {@link #getTier()} defaults to {@link MonsterTier#NORMAL}.
     */
    public MonsterRef(int entityId, int refObjId, int levelOrZero, float distanceToSelf,
            int hpPercentOrNegativeIfUnknown, boolean aggressiveTowardSelf, boolean championOrUnique,
            Optional<Integer> firstAttackerEntityId, Optional<Float> distanceFromTrainingAnchor) {
        this(entityId, refObjId, levelOrZero, distanceToSelf, hpPercentOrNegativeIfUnknown, aggressiveTowardSelf,
                championOrUnique, firstAttackerEntityId, distanceFromTrainingAnchor, MonsterTier.NORMAL);
    }

    public MonsterRef(int entityId, int refObjId, int levelOrZero, float distanceToSelf,
            int hpPercentOrNegativeIfUnknown, boolean aggressiveTowardSelf, boolean championOrUnique,
            Optional<Integer> firstAttackerEntityId, Optional<Float> distanceFromTrainingAnchor, MonsterTier tier) {
        this.entityId = entityId;
        this.refObjId = refObjId;
        this.levelOrZero = levelOrZero;
        this.distanceToSelf = distanceToSelf;
        this.hpPercentOrNegativeIfUnknown = hpPercentOrNegativeIfUnknown;
        this.aggressiveTowardSelf = aggressiveTowardSelf;
        this.championOrUnique = championOrUnique;
        this.firstAttackerEntityId = firstAttackerEntityId != null ? firstAttackerEntityId : Optional.empty();
        this.distanceFromTrainingAnchor = distanceFromTrainingAnchor != null ? distanceFromTrainingAnchor
                : Optional.empty();
        this.tier = tier != null ? tier : MonsterTier.UNKNOWN;
    }

    public MonsterTier getTier() {
        return tier;
    }

    public int getEntityId() {
        return entityId;
    }

    public int getRefObjId() {
        return refObjId;
    }

    public int getLevelOrZero() {
        return levelOrZero;
    }

    public float getDistanceToSelf() {
        return distanceToSelf;
    }

    /**
     * @return 0–100 when known, or a negative sentinel when HP is unknown.
     */
    public int getHpPercentOrNegativeIfUnknown() {
        return hpPercentOrNegativeIfUnknown;
    }

    public boolean isAggressiveTowardSelf() {
        return aggressiveTowardSelf;
    }

    public boolean isChampionOrUnique() {
        return championOrUnique;
    }

    /**
     * When present, the unique id of the first player seen to land a skill on this mob; used for KS protection.
     * Empty when no such event was recorded for this spawn.
     */
    public Optional<Integer> getFirstAttackerEntityId() {
        return firstAttackerEntityId;
    }

    /**
     * World distance from the training leash anchor to the mob, when an anchor is configured; empty when the
     * anchor is not set.
     */
    public Optional<Float> getDistanceFromTrainingAnchor() {
        return distanceFromTrainingAnchor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MonsterRef that = (MonsterRef) o;
        return entityId == that.entityId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId);
    }
}

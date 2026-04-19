package org.sokybot.combat.api;

import java.util.Objects;

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

    public MonsterRef(int entityId, int refObjId, int levelOrZero, float distanceToSelf,
            int hpPercentOrNegativeIfUnknown, boolean aggressiveTowardSelf, boolean championOrUnique) {
        this.entityId = entityId;
        this.refObjId = refObjId;
        this.levelOrZero = levelOrZero;
        this.distanceToSelf = distanceToSelf;
        this.hpPercentOrNegativeIfUnknown = hpPercentOrNegativeIfUnknown;
        this.aggressiveTowardSelf = aggressiveTowardSelf;
        this.championOrUnique = championOrUnique;
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

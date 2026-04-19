package org.sokybot.party.api;

import java.util.Objects;

/**
 * Immutable snapshot of one party member as projected from game events.
 */
public final class PartyMember {

    private final int entityId;
    private final String charName;
    private final int level;
    private final int hpPercent;
    private final int mpPercent;
    private final PartyRole role;
    private final PartyClass partyClass;
    private final float x;
    private final float y;
    private final float z;
    private final long lastUpdateEpochMs;

    public PartyMember(int entityId, String charName, int level, int hpPercent, int mpPercent, PartyRole role,
            PartyClass partyClass, float x, float y, float z, long lastUpdateEpochMs) {
        this.entityId = entityId;
        this.charName = charName != null ? charName : "";
        this.level = level;
        this.hpPercent = hpPercent;
        this.mpPercent = mpPercent;
        this.role = role != null ? role : PartyRole.UNKNOWN;
        this.partyClass = partyClass != null ? partyClass : PartyClass.UNKNOWN;
        this.x = x;
        this.y = y;
        this.z = z;
        this.lastUpdateEpochMs = lastUpdateEpochMs;
    }

    public int getEntityId() {
        return entityId;
    }

    public String getCharName() {
        return charName;
    }

    public int getLevel() {
        return level;
    }

    /** 0–100 when known; negative if unknown. */
    public int getHpPercent() {
        return hpPercent;
    }

    /** 0–100 when known; negative if unknown. */
    public int getMpPercent() {
        return mpPercent;
    }

    public PartyRole getRole() {
        return role;
    }

    public PartyClass getPartyClass() {
        return partyClass;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public long getLastUpdateEpochMs() {
        return lastUpdateEpochMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PartyMember)) {
            return false;
        }
        PartyMember that = (PartyMember) o;
        return entityId == that.entityId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId);
    }
}

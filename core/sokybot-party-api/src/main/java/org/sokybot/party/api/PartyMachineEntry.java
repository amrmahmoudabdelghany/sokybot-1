package org.sokybot.party.api;

import java.util.Objects;

/**
 * World identity and position of a machine's own character (for cross-machine coordination).
 */
public final class PartyMachineEntry {

    private final int entityId;
    private final String charName;
    private final float x;
    private final float y;
    private final float z;

    public PartyMachineEntry(int entityId, String charName, float x, float y, float z) {
        this.entityId = entityId;
        this.charName = charName != null ? charName : "";
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getEntityId() {
        return entityId;
    }

    public String getCharName() {
        return charName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PartyMachineEntry)) {
            return false;
        }
        PartyMachineEntry that = (PartyMachineEntry) o;
        return entityId == that.entityId && Float.compare(that.x, x) == 0 && Float.compare(that.y, y) == 0
                && Float.compare(that.z, z) == 0 && charName.equals(that.charName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, charName, Float.valueOf(x), Float.valueOf(y), Float.valueOf(z));
    }
}

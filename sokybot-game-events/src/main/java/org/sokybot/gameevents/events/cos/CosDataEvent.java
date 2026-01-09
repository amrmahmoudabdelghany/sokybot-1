package org.sokybot.gameevents.events.cos;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when a COS (controlled object) is summoned/spawned.
 * This includes transports, pets (growth, ability, fellow), and job transports.
 * Based on RSBot CosDataResponse (opcode 0x30C8).
 */
public class CosDataEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;

    private final int uniqueId;
    private final int objectId;
    private final CosType cosType;
    private final int hp;
    private final int maxHp;
    private final int ownerUniqueId;

    public CosDataEvent(String fullName, int uniqueId, int objectId,
            CosType cosType, int hp, int maxHp, int ownerUniqueId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.uniqueId = uniqueId;
        this.objectId = objectId;
        this.cosType = cosType;
        this.hp = hp;
        this.maxHp = maxHp;
        this.ownerUniqueId = ownerUniqueId;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public int getObjectId() {
        return objectId;
    }

    public CosType getCosType() {
        return cosType;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getOwnerUniqueId() {
        return ownerUniqueId;
    }

    @Override
    public String toString() {
        return String.format("CosDataEvent{type=%s, uniqueId=%d, objectId=%d, hp=%d/%d}",
                cosType, uniqueId, objectId, hp, maxHp);
    }
}

package org.sokybot.api.events;

/**
 * Event fired when a COS (controlled object) state updates.
 * This handles pet termination, exp gain, hunger, name changes, etc.
 * Based on RSBot CosUpdateResponse (opcode 0x30C9).
 */
public class CosUpdateEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;

    private final int uniqueId;
    private final CosUpdateType updateType;
    private final long experience;
    private final int sourceUniqueId;
    private final int hungerPoints;
    private final String newName;
    private final int newObjectId;

    public CosUpdateEvent(String fullName, int uniqueId, CosUpdateType updateType) {
        this(fullName, uniqueId, updateType, 0, 0, 0, null, 0);
    }

    public CosUpdateEvent(String fullName, int uniqueId, CosUpdateType updateType,
            long experience, int sourceUniqueId, int hungerPoints,
            String newName, int newObjectId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.uniqueId = uniqueId;
        this.updateType = updateType;
        this.experience = experience;
        this.sourceUniqueId = sourceUniqueId;
        this.hungerPoints = hungerPoints;
        this.newName = newName;
        this.newObjectId = newObjectId;
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

    public CosUpdateType getUpdateType() {
        return updateType;
    }

    public long getExperience() {
        return experience;
    }

    public int getSourceUniqueId() {
        return sourceUniqueId;
    }

    public int getHungerPoints() {
        return hungerPoints;
    }

    public String getNewName() {
        return newName;
    }

    public int getNewObjectId() {
        return newObjectId;
    }

    @Override
    public String toString() {
        return String.format("CosUpdateEvent{uniqueId=%d, type=%s}", uniqueId, updateType);
    }
}

package org.sokybot.api.events;

/**
 * Event fired when an entity's HP/MP changes.
 * Based on HPMP_UPDATE packet (0x3057).
 */
public class EntityHPMPUpdateEvent implements IGameEvent {
    
    public enum ChangeType {
        HP_CHANGED,
        MP_CHANGED,
        HP_AND_MP_CHANGED,
        BAD_STATUS,
        HP_AND_BAD_STATUS,
        MP_AND_BAD_STATUS
    }
    
    private final String fullName;
    private final long timestamp;
    private final int entityId;
    private final ChangeType changeType;
    private final Integer newHP;
    private final Integer newMP;
    private final Integer badStatus;
    
    public EntityHPMPUpdateEvent(String machineFullName, int entityId, ChangeType changeType,
                                Integer newHP, Integer newMP, Integer badStatus) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
        this.changeType = changeType;
        this.newHP = newHP;
        this.newMP = newMP;
        this.badStatus = badStatus;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public String getGroupName() {
        return fullName.split("\\.")[0];
    }
    
    @Override
    public String getMachineName() {
        return fullName.split("\\.")[1];
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getEntityId() {
        return entityId;
    }
    
    public ChangeType getChangeType() {
        return changeType;
    }
    
    public Integer getNewHP() {
        return newHP;
    }
    
    public Integer getNewMP() {
        return newMP;
    }
    
    public Integer getBadStatus() {
        return badStatus;
    }
}

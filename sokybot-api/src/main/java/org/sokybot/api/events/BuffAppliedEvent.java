package org.sokybot.api.events;

/**
 * Event fired when a buff/debuff is applied to the character.
 * Important for tracking status effects.
 */
public class BuffAppliedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int buffId;
    private final int casterId;
    private final int duration; // in seconds, 0 = permanent
    
    public BuffAppliedEvent(String machineFullName, int buffId, int casterId, int duration) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.buffId = buffId;
        this.casterId = casterId;
        this.duration = duration;
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
    
    public int getBuffId() {
        return buffId;
    }
    
    public int getCasterId() {
        return casterId;
    }
    
    public int getDuration() {
        return duration;
    }
}

package org.sokybot.api.events;

/**
 * Event fired when a buff/debuff is removed from the character.
 */
public class BuffRemovedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int buffId;
    private final String buffName;  // Name from skill lookup
    
    public BuffRemovedEvent(String machineFullName, int buffId, String buffName) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.buffId = buffId;
        this.buffName = buffName;
    }
    
    // Legacy constructor for backward compatibility
    public BuffRemovedEvent(String machineFullName, int buffId) {
        this(machineFullName, buffId, null);
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
    
    public String getBuffName() {
        return buffName;
    }
}


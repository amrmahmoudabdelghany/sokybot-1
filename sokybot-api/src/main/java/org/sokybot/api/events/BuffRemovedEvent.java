package org.sokybot.api.events;

/**
 * Event fired when a buff/debuff is removed from the character.
 */
public class BuffRemovedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int buffId;
    
    public BuffRemovedEvent(String machineFullName, int buffId) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.buffId = buffId;
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
}

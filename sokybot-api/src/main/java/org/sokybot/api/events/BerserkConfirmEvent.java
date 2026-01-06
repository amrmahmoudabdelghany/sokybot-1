package org.sokybot.api.events;

/**
 * Event fired when berserk mode is confirmed.
 * Based on ServerOpcode.BESERK_CONFIRM (0xB0A7).
 */
public class BerserkConfirmEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final byte berserkLevel;
    
    public BerserkConfirmEvent(String machineFullName, boolean success, byte berserkLevel) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.berserkLevel = berserkLevel;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public String getGroupName() { return fullName.split("\\.")[0]; }
    
    @Override
    public String getMachineName() { return fullName.split("\\.")[1]; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public boolean isSuccess() { return success; }
    public byte getBerserkLevel() { return berserkLevel; }
}

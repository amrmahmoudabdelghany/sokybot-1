package org.sokybot.gameevents.events.stat;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when gold (currency) amount changes.
 * Based on GOLD_UPDATE packet.
 */
public class GoldUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final long newGoldAmount;
    
    public GoldUpdateEvent(String machineFullName, long newGoldAmount) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.newGoldAmount = newGoldAmount;
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
    
    public long getNewGoldAmount() {
        return newGoldAmount;
    }
}

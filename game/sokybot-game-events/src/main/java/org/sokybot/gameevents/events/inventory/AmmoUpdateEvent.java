package org.sokybot.gameevents.events.inventory;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when ammunition count is updated (opcode 0x3201).
 * Arrows/bolts are tracked separately from regular inventory.
 */
public class AmmoUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int ammoCount;
    
    public AmmoUpdateEvent(String fullName, int ammoCount) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.ammoCount = ammoCount;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getAmmoCount() { return ammoCount; }
    
    @Override
    public String toString() {
        return String.format("AmmoUpdateEvent[%s, ammo=%d]", fullName, ammoCount);
    }
}

package org.sokybot.api.events;

/**
 * Event fired for celestial position updates (opcode 0x3020).
 * Represents sun/moon position for day/night cycle.
 */
public class CelestialPositionEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int uniqueId;
    private final short angle;  // Celestial position angle
    
    public CelestialPositionEvent(String fullName, int uniqueId, short angle) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.uniqueId = uniqueId;
        this.angle = angle;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getUniqueId() { return uniqueId; }
    public short getAngle() { return angle; }
    
    @Override
    public String toString() {
        return String.format("CelestialPositionEvent[%s, id=%d, angle=%d]", fullName, uniqueId, angle);
    }
}

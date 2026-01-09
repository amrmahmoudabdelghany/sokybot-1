package org.sokybot.api.events;

/**
 * Event emitted when mount state changes (opcode 0xB0CB).
 */
public class MountStateUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final int ownerUniqueId;
    private final boolean mounted;
    private final int petUniqueId;
    
    public MountStateUpdateEvent(String fullName, int ownerUniqueId, boolean mounted, int petUniqueId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.ownerUniqueId = ownerUniqueId;
        this.mounted = mounted;
        this.petUniqueId = petUniqueId;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getOwnerUniqueId() { return ownerUniqueId; }
    public boolean isMounted() { return mounted; }
    public int getPetUniqueId() { return petUniqueId; }
    
    @Override
    public String toString() {
        return "MountStateUpdateEvent{ownerUniqueId=" + ownerUniqueId + 
               ", mounted=" + mounted + ", petUniqueId=" + petUniqueId + "}";
    }
}

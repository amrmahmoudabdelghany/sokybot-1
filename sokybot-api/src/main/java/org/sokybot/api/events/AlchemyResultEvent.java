package org.sokybot.api.events;

/**
 * Event emitted for Alchemy operations (opcode 0xB150).
 * Handles success, failure, destroy, and cancel scenarios.
 */
public class AlchemyResultEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final boolean success;
    private final byte alchemyType; // Elixir=1, etc.
    private final byte originalSlot;
    
    // Outcome details
    private final boolean destroyed;
    private final boolean cancelled;
    
    // Result item (if success)
    private final Integer newItemId;
    
    public AlchemyResultEvent(String fullName, boolean success, byte alchemyType, byte originalSlot,
            boolean destroyed, boolean cancelled, Integer newItemId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.alchemyType = alchemyType;
        this.originalSlot = originalSlot;
        this.destroyed = destroyed;
        this.cancelled = cancelled;
        this.newItemId = newItemId;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public boolean isSuccess() { return success; }
    public byte getAlchemyType() { return alchemyType; }
    public byte getOriginalSlot() { return originalSlot; }
    public boolean isDestroyed() { return destroyed; }
    public boolean isCancelled() { return cancelled; }
    public Integer getNewItemId() { return newItemId; }
    
    @Override
    public String toString() {
        return "AlchemyResultEvent{success=" + success + 
               ", destroyed=" + destroyed + 
               ", alchemyType=" + alchemyType + "}";
    }
}

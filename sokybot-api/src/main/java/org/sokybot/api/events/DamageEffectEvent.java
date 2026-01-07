package org.sokybot.api.events;

/**
 * Event fired when damage effect numbers appear (opcode 0x3058).
 * Shows damage numbers floating above entities during combat.
 */
public class DamageEffectEvent implements IGameEvent {
    
    public enum DamageType {
        NORMAL,      // Regular damage
        CRITICAL,    // Critical hit
        BLOCK,       // Blocked damage
        ABSORB,      // Absorbed damage
        MISS,        // Missed attack
        HEAL         // Healing effect
    }
    
    private final String fullName;
    private final long timestamp;
    private final int targetEntityId;
    private final int damageAmount;
    private final DamageType damageType;
    
    public DamageEffectEvent(String fullName, int targetEntityId, 
                            int damageAmount, DamageType damageType) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.targetEntityId = targetEntityId;
        this.damageAmount = damageAmount;
        this.damageType = damageType;
    }
    
    public DamageEffectEvent(String fullName, int targetEntityId, 
                            int damageAmount, byte damageTypeCode) {
        this(fullName, targetEntityId, damageAmount, mapDamageType(damageTypeCode));
    }
    
    private static DamageType mapDamageType(byte code) {
        switch (code) {
            case 0: return DamageType.NORMAL;
            case 1: return DamageType.CRITICAL;
            case 2: return DamageType.BLOCK;
            case 3: return DamageType.ABSORB;
            case 4: return DamageType.MISS;
            case 5: return DamageType.HEAL;
            default: return DamageType.NORMAL;
        }
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getTargetEntityId() { return targetEntityId; }
    public int getDamageAmount() { return damageAmount; }
    public DamageType getDamageType() { return damageType; }
    
    @Override
    public String toString() {
        return String.format("DamageEffectEvent[%s, target=%d, damage=%d, type=%s]", 
            fullName, targetEntityId, damageAmount, damageType);
    }
}

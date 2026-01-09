package org.sokybot.gameevents.events.stat;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when entity attack speed updates (opcode 0x3200).
 * Attack speed affects skill/attack animation speed.
 */
public class AttackSpeedUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int entityId;
    private final int attackSpeed;  // Attack speed value
    
    public AttackSpeedUpdateEvent(String fullName, int entityId, int attackSpeed) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
        this.attackSpeed = attackSpeed;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getEntityId() { return entityId; }
    public int getAttackSpeed() { return attackSpeed; }
    
    @Override
    public String toString() {
        return String.format("AttackSpeedUpdateEvent[%s, entity=%d, attackSpeed=%d]", 
            fullName, entityId, attackSpeed);
    }
}

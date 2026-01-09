package org.sokybot.gameevents.events.stat;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when a Fellow pet's stats are updated.
 * Based on RSBot FellowStatUpdateResponse (opcode 0x3422).
 */
public class FellowStatUpdateEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;

    private final int uniqueId;
    private final int strength;
    private final int intelligence;
    private final int physicalAttackMin;
    private final int physicalAttackMax;
    private final int magicalAttackMin;
    private final int magicalAttackMax;
    private final int physicalDefence;
    private final int magicalDefence;
    private final int hitRate;
    private final int maxHealth;

    public FellowStatUpdateEvent(String fullName, int uniqueId,
            int strength, int intelligence,
            int physicalAttackMin, int physicalAttackMax,
            int magicalAttackMin, int magicalAttackMax,
            int physicalDefence, int magicalDefence,
            int hitRate, int maxHealth) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.uniqueId = uniqueId;
        this.strength = strength;
        this.intelligence = intelligence;
        this.physicalAttackMin = physicalAttackMin;
        this.physicalAttackMax = physicalAttackMax;
        this.magicalAttackMin = magicalAttackMin;
        this.magicalAttackMax = magicalAttackMax;
        this.physicalDefence = physicalDefence;
        this.magicalDefence = magicalDefence;
        this.hitRate = hitRate;
        this.maxHealth = maxHealth;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public int getStrength() {
        return strength;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public int getPhysicalAttackMin() {
        return physicalAttackMin;
    }

    public int getPhysicalAttackMax() {
        return physicalAttackMax;
    }

    public int getMagicalAttackMin() {
        return magicalAttackMin;
    }

    public int getMagicalAttackMax() {
        return magicalAttackMax;
    }

    public int getPhysicalDefence() {
        return physicalDefence;
    }

    public int getMagicalDefence() {
        return magicalDefence;
    }

    public int getHitRate() {
        return hitRate;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    @Override
    public String toString() {
        return String.format("FellowStatUpdateEvent{uniqueId=%d, str=%d, int=%d, maxHp=%d}",
                uniqueId, strength, intelligence, maxHealth);
    }
}

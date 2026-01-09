package org.sokybot.gameevents.events.character;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when character combat stats are updated.
 * Contains attack, defense, hit rate, and parry stats.
 */
public class CharacterInfoEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int phyAtkMin;
    private final int phyAtkMax;
    private final int magAtkMin;
    private final int magAtkMax;
    private final int phyDef;
    private final int magDef;
    private final int hitRate;
    private final int parryRate;
    private final int maxHP;
    private final int maxMP;
    private final short strength;  // STR stat points
    private final short intelligence;  // INT stat points
    
    public CharacterInfoEvent(String machineFullName, int phyAtkMin, int phyAtkMax,
                             int magAtkMin, int magAtkMax, int phyDef, int magDef,
                             int hitRate, int parryRate, int maxHP, int maxMP,
                             short strength, short intelligence) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.phyAtkMin = phyAtkMin;
        this.phyAtkMax = phyAtkMax;
        this.magAtkMin = magAtkMin;
        this.magAtkMax = magAtkMax;
        this.phyDef = phyDef;
        this.magDef = magDef;
        this.hitRate = hitRate;
        this.parryRate = parryRate;
        this.maxHP = maxHP;
        this.maxMP = maxMP;
        this.strength = strength;
        this.intelligence = intelligence;
    }
    
    // Legacy constructor for backward compatibility
    public CharacterInfoEvent(String machineFullName, int phyAtkMin, int phyAtkMax,
                             int magAtkMin, int magAtkMax, int phyDef, int magDef,
                             int hitRate, int parryRate, int maxHP, int maxMP) {
        this(machineFullName, phyAtkMin, phyAtkMax, magAtkMin, magAtkMax, phyDef, magDef,
             hitRate, parryRate, maxHP, maxMP, (short)0, (short)0);
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public String getGroupName() { return fullName.split("\\.")[0]; }
    
    @Override
    public String getMachineName() { return fullName.split("\\.")[1]; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getPhyAtkMin() { return phyAtkMin; }
    public int getPhyAtkMax() { return phyAtkMax; }
    public int getMagAtkMin() { return magAtkMin; }
    public int getMagAtkMax() { return magAtkMax; }
    public int getPhyDef() { return phyDef; }
    public int getMagDef() { return magDef; }
    public int getHitRate() { return hitRate; }
    public int getParryRate() { return parryRate; }
    public int getMaxHP() { return maxHP; }
    public int getMaxMP() { return maxMP; }
    public short getStrength() { return strength; }
    public short getIntelligence() { return intelligence; }
}

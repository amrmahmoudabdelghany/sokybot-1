package org.sokybot.gameevents.events.character;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Comprehensive event fired when character data is fully loaded.
 * Based on TrainerHandler.parsingCharData() complete parsing pattern.
 * This is the most important event - signals character is ready.
 */
public class CharacterLoadedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    // Identity
    private final int uniqueId;
    private final int refId;
    private final String characterName;
    
    // Stats
    private final int level;
    private final int maxLevel;
    private final long experience;
    private final long gold;
    private final int skillPoints;
    private final short statPoints;
    private final int currentHP;
    private final int currentMP;
    
    // Combat State
    private final byte lifeState;
    private final byte debuffStatus;
    private final byte motionState;
    private final byte characterStatus;
    private final float walkSpeed;
    private final float runSpeed;
    
    // Position
    private final int xSector;
    private final int ySector;
    private final float xOffset;
    private final float yOffset;
    private final float zOffset;
    private final short angle;
    
    // Inventory counts
    private final int itemCount;
    private final int avatarItemCount;
    private final int masteryCount;
    private final int skillCount;
    private final int activeBuffCount;
    private final int hotKeyCount;
    
    // Job data
    private final String jobName;
    private final byte jobType;
    private final byte jobLevel;
    private final int jobExp;
    
    // PvP and status
    private final byte pvpState;
    private final boolean hasTransport;
    private final boolean inCombat;
    
    public CharacterLoadedEvent(String machineFullName, int uniqueId, int refId,
                                String characterName, int level, int maxLevel,
                                long experience, long gold, int skillPoints, short statPoints,
                                int currentHP, int currentMP,
                                byte lifeState, byte debuffStatus, byte motionState, byte characterStatus,
                                float walkSpeed, float runSpeed,
                                int xSector, int ySector, float xOffset, float yOffset, float zOffset, short angle,
                                int itemCount, int avatarItemCount, int masteryCount, int skillCount,
                                int activeBuffCount, int hotKeyCount,
                                String jobName, byte jobType, byte jobLevel, int jobExp,
                                byte pvpState, boolean hasTransport, boolean inCombat) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.uniqueId = uniqueId;
        this.refId = refId;
        this.characterName = characterName;
        this.level = level;
        this.maxLevel = maxLevel;
        this.experience = experience;
        this.gold = gold;
        this.skillPoints = skillPoints;
        this.statPoints = statPoints;
        this.currentHP = currentHP;
        this.currentMP = currentMP;
        this.lifeState = lifeState;
        this.debuffStatus = debuffStatus;
        this.motionState = motionState;
        this.characterStatus = characterStatus;
        this.walkSpeed = walkSpeed;
        this.runSpeed = runSpeed;
        this.xSector = xSector;
        this.ySector = ySector;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
        this.angle = angle;
        this.itemCount = itemCount;
        this.avatarItemCount = avatarItemCount;
        this.masteryCount = masteryCount;
        this.skillCount = skillCount;
        this.activeBuffCount = activeBuffCount;
        this.hotKeyCount = hotKeyCount;
        this.jobName = jobName;
        this.jobType = jobType;
        this.jobLevel = jobLevel;
        this.jobExp = jobExp;
        this.pvpState = pvpState;
        this.hasTransport = hasTransport;
        this.inCombat = inCombat;
    }
    
    // IGameEvent implementation
    @Override
    public String getFullName() { return fullName; }
    @Override
    public String getGroupName() { return fullName.split("\\.")[0]; }
    @Override
    public String getMachineName() { return fullName.split("\\.")[1]; }
    @Override
    public long getTimestamp() { return timestamp; }
    
    // Identity
    public int getUniqueId() { return uniqueId; }
    public int getRefId() { return refId; }
    public String getCharacterName() { return characterName; }
    
    // Stats
    public int getLevel() { return level; }
    public int getMaxLevel() { return maxLevel; }
    public long getExperience() { return experience; }
    public long getGold() { return gold; }
    public int getSkillPoints() { return skillPoints; }
    public short getStatPoints() { return statPoints; }
    public int getCurrentHP() { return currentHP; }
    public int getCurrentMP() { return currentMP; }
    
    // Combat State
    public byte getLifeState() { return lifeState; }
    public byte getDebuffStatus() { return debuffStatus; }
    public byte getMotionState() { return motionState; }
    public byte getCharacterStatus() { return characterStatus; }
    public float getWalkSpeed() { return walkSpeed; }
    public float getRunSpeed() { return runSpeed; }
    
    // Position
    public int getXSector() { return xSector; }
    public int getYSector() { return ySector; }
    public float getXOffset() { return xOffset; }
    public float getYOffset() { return yOffset; }
    public float getZOffset() { return zOffset; }
    public short getAngle() { return angle; }
    
    // Inventory counts
    public int getItemCount() { return itemCount; }
    public int getAvatarItemCount() { return avatarItemCount; }
    public int getMasteryCount() { return masteryCount; }
    public int getSkillCount() { return skillCount; }
    public int getActiveBuffCount() { return activeBuffCount; }
    public int getHotKeyCount() { return hotKeyCount; }
    
    // Job
    public String getJobName() { return jobName; }
    public byte getJobType() { return jobType; }
    public byte getJobLevel() { return jobLevel; }
    public int getJobExp() { return jobExp; }
    
    // Status
    public byte getPvpState() { return pvpState; }
    public boolean isHasTransport() { return hasTransport; }
    public boolean isInCombat() { return inCombat; }
}

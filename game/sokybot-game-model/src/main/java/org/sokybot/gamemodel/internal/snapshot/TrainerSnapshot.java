package org.sokybot.gamemodel.internal.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sokybot.gameevents.dto.Buff;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.dto.HotKey;
import org.sokybot.gameevents.dto.Mastery;
import org.sokybot.gameevents.dto.Skill;
import org.sokybot.gameevents.enums.CharacterStatus;
import org.sokybot.gameevents.enums.DebuffStatus;
import org.sokybot.gameevents.enums.FreePVP;
import org.sokybot.gameevents.enums.JobType;
import org.sokybot.gameevents.enums.LifeState;
import org.sokybot.gameevents.enums.MotionState;
import org.sokybot.gameevents.enums.MovementType;
import org.sokybot.gameevents.enums.PVPState;
import org.sokybot.gameevents.enums.SkillCastErrorType;
import org.sokybot.gamemodel.snapshot.IItemSnapshot;
import org.sokybot.gamemodel.snapshot.ITrainerSnapshot;

public final class TrainerSnapshot implements ITrainerSnapshot {
    private final int uniqueId;
    private final int refId;
    private final String name;
    private final int xSector;
    private final int ySector;
    private final float xOffset;
    private final float yOffset;
    private final float zOffset;
    private final short angle;
    private final GamePosition position;
    private final int x;
    private final int y;
    private final boolean hasDestination;
    private final MovementType movementType;
    private final LifeState lifeState;
    private final DebuffStatus debuffStatus;
    private final MotionState motionState;
    private final CharacterStatus characterStatus;
    private final float walkSpeed;
    private final float runSpeed;
    private final float hwanSpeed;
    private final int currentHP;
    private final int currentMP;
    private final int maxHP;
    private final int maxMP;
    private final int destX;
    private final int destY;
    private final byte destXSector;
    private final byte destYSector;
    private final short destXOffset;
    private final short destYOffset;
    private final short destZOffset;
    private final int targetId;
    private final byte level;
    private final byte maxLvl;
    private final long charEXPOffset;
    private final int sExpOffset;
    private final long gold;
    private final int skillPoint;
    private final short charStatPoint;
    private final byte zerkCount;
    private final byte dailyPK;
    private final short totalPK;
    private final int pkPenaltyPoint;
    private final byte zerkLvl;
    private final FreePVP freePVP;
    private final PVPState pvpState;
    private final byte pvpFlag;
    private final String jobName;
    private final JobType jobType;
    private final byte jobLvl;
    private final int jobExp;
    private final int jobContribution;
    private final int jobReward;
    private final byte itemInventorySize;
    private final byte itemCount;
    private final List<IItemSnapshot> inventory;
    private final byte avaterInventorySize;
    private final byte avaterItemCount;
    private final List<IItemSnapshot> avatarInventory;
    private final boolean hasMask;
    private final List<Mastery> mastryList;
    private final List<Skill> skills;
    private final List<Integer> completedQuests;
    private final List<Object> activeQuests;
    private final int serverTime;
    private final byte charScale;
    private final List<HotKey> hotKeys;
    private final List<Buff> buffs;
    private final int phyAtkMin;
    private final int phyAtkMax;
    private final int magAtkMin;
    private final int magAtkMax;
    private final short phyDef;
    private final short magDef;
    private final short hitRate;
    private final short parryRate;
    private final short charSTR;
    private final short charINT;
    private final SkillCastErrorType lastError;
    private final int lastErrorTargetId;

    private TrainerSnapshot(org.sokybot.gamemodel.model.ITrainer live) {
        this.uniqueId = live.getUniqueId();
        this.refId = live.getRefId();
        this.name = live.getName();
        this.xSector = live.getXSector();
        this.ySector = live.getYSector();
        this.xOffset = live.getXOffset();
        this.yOffset = live.getYOffset();
        this.zOffset = live.getZOffset();
        this.angle = live.getAngle();
        this.position = live.getPosition();
        this.x = live.getX();
        this.y = live.getY();
        this.hasDestination = live.isHasDestination();
        this.movementType = live.getMovementType();
        this.lifeState = live.getLifeState();
        this.debuffStatus = live.getDebuffStatus();
        this.motionState = live.getMotionState();
        this.characterStatus = live.getCharacterStatus();
        this.walkSpeed = live.getWalkSpeed();
        this.runSpeed = live.getRunSpeed();
        this.hwanSpeed = live.getHwanSpeed();
        this.currentHP = live.getCurrentHP();
        this.currentMP = live.getCurrentMP();
        this.maxHP = live.getMaxHP();
        this.maxMP = live.getMaxMP();
        this.destX = live.getDestX();
        this.destY = live.getDestY();
        this.destXSector = live.getDestXSector();
        this.destYSector = live.getDestYSector();
        this.destXOffset = live.getDestXOffset();
        this.destYOffset = live.getDestYOffset();
        this.destZOffset = live.getDestZOffset();
        this.targetId = live.getTargetId();
        this.level = live.getLevel();
        this.maxLvl = live.getMaxLvl();
        this.charEXPOffset = live.getCharEXPOffset();
        this.sExpOffset = live.getSExpOffset();
        this.gold = live.getGold();
        this.skillPoint = live.getSkillPoint();
        this.charStatPoint = live.getCharStatPoint();
        this.zerkCount = live.getZerkCount();
        this.dailyPK = live.getDailyPK();
        this.totalPK = live.getTotalPK();
        this.pkPenaltyPoint = live.getPkPenaltyPoint();
        this.zerkLvl = live.getZerkLvl();
        this.freePVP = live.getFreePVP();
        this.pvpState = live.getPvpState();
        this.pvpFlag = live.getPvpFlag();
        this.jobName = live.getJobName();
        this.jobType = live.getJobType();
        this.jobLvl = live.getJobLvl();
        this.jobExp = live.getJobExp();
        this.jobContribution = live.getJobContribution();
        this.jobReward = live.getJobReward();
        this.itemInventorySize = live.getItemInventorySize();
        this.itemCount = live.getItemCount();
        this.inventory = toFrozenItems(live.getInventory());
        this.avaterInventorySize = live.getAvaterInventorySize();
        this.avaterItemCount = live.getAvaterItemCount();
        this.avatarInventory = toFrozenItems(live.getAvatarInventory());
        this.hasMask = live.isHasMask();
        this.mastryList = Collections.unmodifiableList(new ArrayList<>(live.getMastryList()));
        this.skills = Collections.unmodifiableList(new ArrayList<>(live.getSkills()));
        this.completedQuests = Collections.unmodifiableList(new ArrayList<>(live.getCompletedQuests()));
        this.activeQuests = Collections.unmodifiableList(new ArrayList<>(live.getActiveQuests()));
        this.serverTime = live.getServerTime();
        this.charScale = live.getCharScale();
        this.hotKeys = Collections.unmodifiableList(new ArrayList<>(live.getHotKeys()));
        this.buffs = Collections.unmodifiableList(new ArrayList<>(live.getBuffs()));
        this.phyAtkMin = live.getPhyAtkMin();
        this.phyAtkMax = live.getPhyAtkMax();
        this.magAtkMin = live.getMagAtkMin();
        this.magAtkMax = live.getMagAtkMax();
        this.phyDef = live.getPhyDef();
        this.magDef = live.getMagDef();
        this.hitRate = live.getHitRate();
        this.parryRate = live.getParryRate();
        this.charSTR = live.getCharSTR();
        this.charINT = live.getCharINT();
        this.lastError = live.getLastError();
        this.lastErrorTargetId = live.getLastErrorTargetId();
    }

    private static List<IItemSnapshot> toFrozenItems(List<? extends org.sokybot.gamemodel.model.IItem> items) {
        List<IItemSnapshot> copied = new ArrayList<>();
        for (org.sokybot.gamemodel.model.IItem item : items) {
            copied.add(ItemSnapshot.of(item));
        }
        return Collections.unmodifiableList(copied);
    }

    public static TrainerSnapshot of(org.sokybot.gamemodel.model.ITrainer live) {
        return new TrainerSnapshot(live);
    }

    @Override public int getUniqueId() { return uniqueId; }
    @Override public int getRefId() { return refId; }
    @Override public String getName() { return name; }
    @Override public int getXSector() { return xSector; }
    @Override public int getYSector() { return ySector; }
    @Override public float getXOffset() { return xOffset; }
    @Override public float getYOffset() { return yOffset; }
    @Override public float getZOffset() { return zOffset; }
    @Override public short getAngle() { return angle; }
    @Override public GamePosition getPosition() { return position; }
    @Override public int getX() { return x; }
    @Override public int getY() { return y; }
    @Override public boolean isHasDestination() { return hasDestination; }
    @Override public MovementType getMovementType() { return movementType; }
    @Override public LifeState getLifeState() { return lifeState; }
    @Override public boolean isAlive() { return lifeState == LifeState.Alive; }
    @Override public DebuffStatus getDebuffStatus() { return debuffStatus; }
    @Override public MotionState getMotionState() { return motionState; }
    @Override public CharacterStatus getCharacterStatus() { return characterStatus; }
    @Override public float getWalkSpeed() { return walkSpeed; }
    @Override public float getRunSpeed() { return runSpeed; }
    @Override public float getHwanSpeed() { return hwanSpeed; }
    @Override public int getCurrentHP() { return currentHP; }
    @Override public int getCurrentMP() { return currentMP; }
    @Override public int getMaxHP() { return maxHP; }
    @Override public int getMaxMP() { return maxMP; }
    @Override public int getHPPercentage() { return maxHP == 0 ? 0 : (int) ((currentHP * 100L) / maxHP); }
    @Override public int getMPPercentage() { return maxMP == 0 ? 0 : (int) ((currentMP * 100L) / maxMP); }
    @Override public int getDestX() { return destX; }
    @Override public int getDestY() { return destY; }
    @Override public byte getDestXSector() { return destXSector; }
    @Override public byte getDestYSector() { return destYSector; }
    @Override public short getDestXOffset() { return destXOffset; }
    @Override public short getDestYOffset() { return destYOffset; }
    @Override public short getDestZOffset() { return destZOffset; }
    @Override public int getTargetId() { return targetId; }
    @Override public byte getLevel() { return level; }
    @Override public byte getMaxLvl() { return maxLvl; }
    @Override public long getCharEXPOffset() { return charEXPOffset; }
    @Override public int getSExpOffset() { return sExpOffset; }
    @Override public long getGold() { return gold; }
    @Override public int getSkillPoint() { return skillPoint; }
    @Override public short getCharStatPoint() { return charStatPoint; }
    @Override public byte getZerkCount() { return zerkCount; }
    @Override public byte getDailyPK() { return dailyPK; }
    @Override public short getTotalPK() { return totalPK; }
    @Override public int getPkPenaltyPoint() { return pkPenaltyPoint; }
    @Override public byte getZerkLvl() { return zerkLvl; }
    @Override public FreePVP getFreePVP() { return freePVP; }
    @Override public PVPState getPvpState() { return pvpState; }
    @Override public byte getPvpFlag() { return pvpFlag; }
    @Override public String getJobName() { return jobName; }
    @Override public JobType getJobType() { return jobType; }
    @Override public byte getJobLvl() { return jobLvl; }
    @Override public int getJobExp() { return jobExp; }
    @Override public int getJobContribution() { return jobContribution; }
    @Override public int getJobReward() { return jobReward; }
    @Override public byte getItemInventorySize() { return itemInventorySize; }
    @Override public byte getItemCount() { return itemCount; }
    @Override public List<IItemSnapshot> getInventory() { return inventory; }
    @Override public byte getAvaterInventorySize() { return avaterInventorySize; }
    @Override public byte getAvaterItemCount() { return avaterItemCount; }
    @Override public List<IItemSnapshot> getAvatarInventory() { return avatarInventory; }
    @Override public boolean isHasMask() { return hasMask; }
    @Override public List<Mastery> getMastryList() { return mastryList; }
    @Override public List<Skill> getSkills() { return skills; }
    @Override public List<Integer> getCompletedQuests() { return completedQuests; }
    @Override public List<Object> getActiveQuests() { return activeQuests; }
    @Override public int getServerTime() { return serverTime; }
    @Override public byte getCharScale() { return charScale; }
    @Override public List<HotKey> getHotKeys() { return hotKeys; }
    @Override public List<Buff> getBuffs() { return buffs; }
    @Override public int getPhyAtkMin() { return phyAtkMin; }
    @Override public int getPhyAtkMax() { return phyAtkMax; }
    @Override public int getMagAtkMin() { return magAtkMin; }
    @Override public int getMagAtkMax() { return magAtkMax; }
    @Override public short getPhyDef() { return phyDef; }
    @Override public short getMagDef() { return magDef; }
    @Override public short getHitRate() { return hitRate; }
    @Override public short getParryRate() { return parryRate; }
    @Override public short getCharSTR() { return charSTR; }
    @Override public short getCharINT() { return charINT; }
    @Override public boolean hasSkill(int skillId) { return skills.stream().anyMatch(s -> s.getRefId() == skillId); }
    @Override public Optional<Skill> findSkill(String name) {
        return skills.stream().filter(s -> s.getName() != null && s.getName().equalsIgnoreCase(name)).findFirst();
    }
    @Override public SkillCastErrorType getLastError() { return lastError; }
    @Override public int getLastErrorTargetId() { return lastErrorTargetId; }

    @Override
    public double distance(int tx, int ty) {
        return Math.sqrt(Math.pow(this.x - tx, 2) + Math.pow(this.y - ty, 2));
    }

    @Override
    public double distance(float tx, float ty) {
        return Math.sqrt(Math.pow(this.x - tx, 2) + Math.pow(this.y - ty, 2));
    }
}

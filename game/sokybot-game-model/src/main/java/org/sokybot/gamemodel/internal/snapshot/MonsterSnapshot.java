package org.sokybot.gamemodel.internal.snapshot;

import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.enums.CharacterStatus;
import org.sokybot.gameevents.enums.DebuffStatus;
import org.sokybot.gameevents.enums.LifeState;
import org.sokybot.gameevents.enums.MonsterType;
import org.sokybot.gameevents.enums.MotionState;
import org.sokybot.gameevents.enums.MovementType;
import org.sokybot.gamemodel.model.IMonster;
import org.sokybot.gamemodel.snapshot.IMonsterSnapshot;

public final class MonsterSnapshot implements IMonsterSnapshot {
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
    private final MonsterType monsterType;
    private final int level;

    private MonsterSnapshot(IMonster live) {
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
        this.monsterType = live.getMonsterType();
        this.level = live.getLevel();
    }

    public static MonsterSnapshot of(IMonster live) {
        return new MonsterSnapshot(live);
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
    @Override public MonsterType getMonsterType() { return monsterType; }
    @Override public int getLevel() { return level; }

    @Override
    public double distance(int tx, int ty) {
        return Math.sqrt(Math.pow(this.x - tx, 2) + Math.pow(this.y - ty, 2));
    }

    @Override
    public double distance(float tx, float ty) {
        return Math.sqrt(Math.pow(this.x - tx, 2) + Math.pow(this.y - ty, 2));
    }
}

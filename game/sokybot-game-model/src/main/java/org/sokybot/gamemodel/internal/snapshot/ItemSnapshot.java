package org.sokybot.gamemodel.internal.snapshot;

import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gamemodel.model.IItem;
import org.sokybot.gamemodel.snapshot.IItemSnapshot;

public final class ItemSnapshot implements IItemSnapshot {
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
    private final byte slot;
    private final int rentType;
    private final short stackCount;
    private final byte attributeAssimilationProbability;
    private final String longId;
    private final boolean weapon;
    private final boolean shield;
    private final boolean accessory;
    private final byte level;

    private ItemSnapshot(IItem live) {
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
        this.slot = live.getSlot();
        this.rentType = live.getRentType();
        this.stackCount = live.getStackCount();
        this.attributeAssimilationProbability = live.getAttributeAssimilationProbability();
        this.longId = live.getLongId();
        this.weapon = live.isWeapon();
        this.shield = live.isShield();
        this.accessory = live.isAccessory();
        this.level = live.getLevel();
    }

    public static ItemSnapshot of(IItem live) {
        return new ItemSnapshot(live);
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
    @Override public byte getSlot() { return slot; }
    @Override public int getRentType() { return rentType; }
    @Override public short getStackCount() { return stackCount; }
    @Override public byte getAttributeAssimilationProbability() { return attributeAssimilationProbability; }
    @Override public String getLongId() { return longId; }
    @Override public boolean isWeapon() { return weapon; }
    @Override public boolean isShield() { return shield; }
    @Override public boolean isAccessory() { return accessory; }
    @Override public byte getLevel() { return level; }

    @Override
    public double distance(int tx, int ty) {
        return Math.sqrt(Math.pow(this.x - tx, 2) + Math.pow(this.y - ty, 2));
    }

    @Override
    public double distance(float tx, float ty) {
        return Math.sqrt(Math.pow(this.x - tx, 2) + Math.pow(this.y - ty, 2));
    }
}

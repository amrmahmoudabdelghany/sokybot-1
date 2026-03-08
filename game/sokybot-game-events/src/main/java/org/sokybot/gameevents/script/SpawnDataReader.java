package org.sokybot.gameevents.script;

import org.sokybot.commons.SilkroadUtils;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.dto.ItemData;
import org.sokybot.gameevents.dto.MonsterData;
import org.sokybot.gameevents.enums.MonsterType;
import org.sokybot.gameevents.enums.Rarity;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.service.IGameDataLookup;

import lombok.RequiredArgsConstructor;

/**
 * Stateless utility to parse spawn packets into DTOs.
 * Used by entity spawn and group spawn translators.
 */
@RequiredArgsConstructor
public class SpawnDataReader {

    private final IStreamReader reader;
    private final IGameDataLookup lookup;

    /** Immutable spawn position read from the shared spawn header. */
    public static final class SpawnPosition {
        private final int uniqueId;
        private final int xSector;
        private final int ySector;
        private final float xOffset;
        private final float yOffset;
        private final float zOffset;
        private final short angle;
        private final GamePosition position;

        public SpawnPosition(int uniqueId, int xSector, int ySector, float xOffset, float yOffset,
                float zOffset, short angle, GamePosition position) {
            this.uniqueId = uniqueId;
            this.xSector = xSector;
            this.ySector = ySector;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.zOffset = zOffset;
            this.angle = angle;
            this.position = position;
        }

        public int uniqueId() { return uniqueId; }
        public int xSector() { return xSector; }
        public int ySector() { return ySector; }
        public float xOffset() { return xOffset; }
        public float yOffset() { return yOffset; }
        public float zOffset() { return zOffset; }
        public short angle() { return angle; }
        public GamePosition position() { return position; }
    }

    /**
     * Read the shared spawn header (uniqueId, sectors, offsets, angle, world position).
     */
    public SpawnPosition readSpawnPosition() {
        int uniqueId = reader.getInt();
        int xSector = reader.getUnsignedByte();
        int ySector = reader.getUnsignedByte();
        float xOffset = reader.getFloat();
        float zOffset = reader.getFloat();
        float yOffset = reader.getFloat();
        short angle = (short) SilkroadUtils.getAngle(reader.getShort());
        float worldX = SilkroadUtils.getXCoord(xOffset, (short) xSector, 10);
        float worldY = SilkroadUtils.getYCoord(yOffset, (short) ySector, 10);
        GamePosition position = new GamePosition(worldX, worldY, zOffset, (byte) 0);
        return new SpawnPosition(uniqueId, xSector, ySector, xOffset, yOffset, zOffset, angle, position);
    }

    /**
     * Read and consume the fighter state block (destination/movement, life state, speeds, buffs).
     * Must be called after readSpawnPosition() when parsing monster/player spawns.
     */
    public void readFighterState() {
        PacketReaderUtils.skipFighterMovementBlock(reader);
        reader.getByte(); // LifeState
        reader.getByte(); // DebuffStatus
        reader.getByte(); // MotionState
        reader.getByte(); // CharacterStatus
        reader.getFloat(); // WalkSpeed
        reader.getFloat(); // RunSpeed
        reader.getFloat(); // HwanSpeed
        byte buffCount = reader.getByte();
        for (int i = 0; i < buffCount; i++) {
            int skillId = reader.getInt();
            reader.getInt(); // duration
            if (lookup != null) {
                lookup.findSkill(skillId).ifPresent(s -> {
                    if (PacketReaderUtils.isTransferableBuff(s.getLongId())) {
                        reader.getBoolean(); // isCreator
                    }
                });
            }
        }
    }

    public MonsterData readMonster(NPCEntity entity) {
        SpawnPosition pos = readSpawnPosition();
        readFighterState();
        reader.getByte(); // Talk flag
        reader.getByte(); // Rarity
        reader.getByte(); // Appearance
        byte strengthLevel = reader.getByte();
        MonsterType type = MonsterType.Normal;
        switch (strengthLevel) {
            case 1:
                type = MonsterType.Champion;
                break;
            case 4:
                type = MonsterType.Giant;
                break;
            default:
                break;
        }
        int maxHp = entity.getHP();
        if (type == MonsterType.Champion) {
            maxHp *= 2;
        } else if (type == MonsterType.Giant) {
            maxHp *= 5;
        }
        return MonsterData.builder()
                .uniqueId(pos.uniqueId())
                .refId(entity.getRefId())
                .name(entity.getName())
                .xSector(pos.xSector())
                .ySector(pos.ySector())
                .xOffset(pos.xOffset())
                .yOffset(pos.yOffset())
                .zOffset(pos.zOffset())
                .angle(pos.angle())
                .position(pos.position())
                .monsterType(type)
                .maxHp(maxHp)
                .currentHp(maxHp)
                .level(entity.getLevel())
                .build();
    }

    public ItemData readDropItem(ItemEntity entity) {
        String longId = entity.getLongId();
        int amount = 1;
        if (longId.startsWith("ITEM_ETC_GOLD")) {
            amount = reader.getInt();
        } else if (longId.startsWith("ITEM_QSP")
                || longId.startsWith("ITEM_ETC_E090825")
                || longId.startsWith("ITEM_QNO")
                || longId.startsWith("ITEM_TRADE_SPECIAL_BOX")) {
            reader.getString();
        }
        byte plus = 0;
        if (longId.startsWith("ITEM_CH") || longId.startsWith("ITEM_EU")) {
            plus = reader.getByte();
        }
        SpawnPosition pos = readSpawnPosition();
        boolean ownerExist = reader.getByte() == 1;
        int ownerId = ownerExist ? reader.getInt() : 0;
        Rarity rarity = Rarity.of(reader.getByte());
        return ItemData.builder()
                .uniqueId(pos.uniqueId())
                .refId(entity.getRefId())
                .name(entity.getName())
                .xSector(pos.xSector())
                .ySector(pos.ySector())
                .xOffset(pos.xOffset())
                .yOffset(pos.yOffset())
                .zOffset(pos.zOffset())
                .angle(pos.angle())
                .position(pos.position())
                .amount(amount)
                .ownerExist(ownerExist)
                .ownerJID(ownerId)
                .plus(plus)
                .rarity(rarity)
                .build();
    }
}

package org.sokybot.gameevents.script;

import org.sokybot.network.packet.IStreamReader;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Parses the shared character data packet structure (opcode 0x3013 / 0x34A6).
 * Used by CharacterLoadedTranslator and CharacterDataEndTranslator with different handlers.
 */
public final class CharacterDataParser {

    private CharacterDataParser() {}

    /**
     * Result of parsing character data (all fields needed to build CharacterLoadedEvent).
     */
    public static final class CharacterData {
        private final int refId;
        private final int level;
        private final int maxLevel;
        private final long experience;
        private final long gold;
        private final int skillPoints;
        private final short statPoints;
        private final int currentHP;
        private final int currentMP;
        private final byte lifeState;
        private final byte debuffStatus;
        private final byte motionState;
        private final byte characterStatus;
        private final float walkSpeed;
        private final float runSpeed;
        private final int uniqueId;
        private final int xSector;
        private final int ySector;
        private final float xOffset;
        private final float yOffset;
        private final float zOffset;
        private final short angle;
        private final int inventoryItemCount;
        private final int avatarItemCount;
        private final int masteryCount;
        private final int skillCount;
        private final int activeBuffCount;
        private final int hotKeyCount;
        private final String characterName;
        private final String jobName;
        private final byte jobType;
        private final byte jobLevel;
        private final int jobExp;
        private final byte pvpState;
        private final boolean hasTransport;
        private final boolean inCombat;

        public CharacterData(int refId, int level, int maxLevel, long experience, long gold,
                int skillPoints, short statPoints, int currentHP, int currentMP,
                byte lifeState, byte debuffStatus, byte motionState, byte characterStatus,
                float walkSpeed, float runSpeed, int uniqueId, int xSector, int ySector,
                float xOffset, float yOffset, float zOffset, short angle,
                int inventoryItemCount, int avatarItemCount, int masteryCount, int skillCount,
                int activeBuffCount, int hotKeyCount, String characterName, String jobName,
                byte jobType, byte jobLevel, int jobExp, byte pvpState, boolean hasTransport, boolean inCombat) {
            this.refId = refId;
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
            this.uniqueId = uniqueId;
            this.xSector = xSector;
            this.ySector = ySector;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.zOffset = zOffset;
            this.angle = angle;
            this.inventoryItemCount = inventoryItemCount;
            this.avatarItemCount = avatarItemCount;
            this.masteryCount = masteryCount;
            this.skillCount = skillCount;
            this.activeBuffCount = activeBuffCount;
            this.hotKeyCount = hotKeyCount;
            this.characterName = characterName;
            this.jobName = jobName;
            this.jobType = jobType;
            this.jobLevel = jobLevel;
            this.jobExp = jobExp;
            this.pvpState = pvpState;
            this.hasTransport = hasTransport;
            this.inCombat = inCombat;
        }

        public int refId() { return refId; }
        public int level() { return level; }
        public int maxLevel() { return maxLevel; }
        public long experience() { return experience; }
        public long gold() { return gold; }
        public int skillPoints() { return skillPoints; }
        public short statPoints() { return statPoints; }
        public int currentHP() { return currentHP; }
        public int currentMP() { return currentMP; }
        public byte lifeState() { return lifeState; }
        public byte debuffStatus() { return debuffStatus; }
        public byte motionState() { return motionState; }
        public byte characterStatus() { return characterStatus; }
        public float walkSpeed() { return walkSpeed; }
        public float runSpeed() { return runSpeed; }
        public int uniqueId() { return uniqueId; }
        public int xSector() { return xSector; }
        public int ySector() { return ySector; }
        public float xOffset() { return xOffset; }
        public float yOffset() { return yOffset; }
        public float zOffset() { return zOffset; }
        public short angle() { return angle; }
        public int inventoryItemCount() { return inventoryItemCount; }
        public int avatarItemCount() { return avatarItemCount; }
        public int masteryCount() { return masteryCount; }
        public int skillCount() { return skillCount; }
        public int activeBuffCount() { return activeBuffCount; }
        public int hotKeyCount() { return hotKeyCount; }
        public String characterName() { return characterName; }
        public String jobName() { return jobName; }
        public byte jobType() { return jobType; }
        public byte jobLevel() { return jobLevel; }
        public int jobExp() { return jobExp; }
        public byte pvpState() { return pvpState; }
        public boolean hasTransport() { return hasTransport; }
        public boolean inCombat() { return inCombat; }
    }

    public interface Handlers {
        void onInventoryItem(byte slot, int itemId);

        void onAvatarItem(byte slot, int itemId);

        void onMastery(int masteryId, int masteryLevel);

        void onSkill(int skillRefId, boolean isEnabled);

        void onBuff(int buffRefId, int duration);
    }

    /**
     * Parse character data from the reader, calling handlers for items/masteries/skills/buffs.
     * Handlers may be null to skip (no-op).
     */
    public static CharacterData parse(IStreamReader reader, IGameDataLookup lookup, Handlers handlers) {
        reader.getInt(); // serverTime
        int refId = reader.getInt();
        reader.getByte(); // charScale
        int level = reader.getByte() & 0xFF;
        int maxLevel = reader.getByte() & 0xFF;
        long experience = reader.getLong();
        reader.getInt(); // sexpOffset
        long gold = reader.getLong();
        int skillPoints = reader.getInt();
        short statPoints = reader.getShort();
        reader.getByte(); // zerkCount
        reader.getInt(); // gatheredExpPoint
        int currentHP = reader.getInt();
        int currentMP = reader.getInt();
        reader.getByte(); // autoInvestExp
        reader.getByte(); // dailyPK
        reader.getShort(); // totalPK
        reader.getInt(); // pkPenaltyPoint
        reader.getByte(); // zerkLvl
        reader.getByte(); // freePVP

        reader.getByte(); // itemInventorySize
        int itemCount = reader.getByte() & 0xFF;
        for (int i = 0; i < itemCount; i++) {
            byte slot = reader.getByte();
            int rentType = reader.getInt();
            switch (rentType) {
                case 1:
                    reader.getShort();
                    reader.getInt();
                    reader.getInt();
                    break;
                case 2:
                    reader.getShort();
                    reader.getShort();
                    reader.getInt();
                    break;
                case 3:
                    reader.getShort();
                    reader.getInt();
                    reader.getInt();
                    reader.getShort();
                    reader.getInt();
                    break;
                default:
                    break;
            }
            int itemId = reader.getInt();
            if (handlers != null) {
                handlers.onInventoryItem(slot, itemId);
            }
        }
        reader.getByte(); // avatarInventorySize
        int avatarItemCount = reader.getByte() & 0xFF;
        for (int i = 0; i < avatarItemCount; i++) {
            byte slot = reader.getByte();
            int rentType = reader.getInt();
            switch (rentType) {
                case 1:
                    reader.getShort();
                    reader.getInt();
                    reader.getInt();
                    break;
                case 2:
                    reader.getShort();
                    reader.getShort();
                    reader.getInt();
                    break;
                case 3:
                    reader.getShort();
                    reader.getInt();
                    reader.getInt();
                    reader.getShort();
                    reader.getInt();
                    break;
                default:
                    break;
            }
            int itemId = reader.getInt();
            if (handlers != null) {
                handlers.onAvatarItem(slot, itemId);
            }
        }
        reader.getBoolean(); // hasMask

        int masteryCount = 0;
        while (reader.getBoolean()) {
            int masteryId = reader.getInt();
            int masteryLevel = reader.getByte() & 0xFF;
            masteryCount++;
            if (handlers != null) {
                handlers.onMastery(masteryId, masteryLevel);
            }
        }
        reader.getByte(); // mastery end byte

        int skillRefId = reader.getInt();
        boolean isEnabled = reader.getByte() == 1;
        int skillCount = 1;
        if (handlers != null) {
            handlers.onSkill(skillRefId, isEnabled);
        }

        short completedQuestCount = reader.getShort();
        for (int i = 0; i < completedQuestCount; i++) {
            reader.getInt();
        }
        int activeQuestCount = reader.getByte() & 0xFF;
        for (int i = 0; i < activeQuestCount; i++) {
            PacketReaderUtils.skipQuest(reader);
        }
        reader.getByte(); // unk05

        int collectionBookCount = reader.getInt();
        for (int i = 0; i < collectionBookCount; i++) {
            reader.getInt();
            reader.getInt();
            reader.getInt();
        }

        int uniqueId = reader.getInt();
        int xSector = reader.getUnsignedByte();
        int ySector = reader.getUnsignedByte();
        float xOffset = reader.getFloat();
        float zOffset = reader.getFloat();
        float yOffset = reader.getFloat();
        short angle = reader.getShort();

        PacketReaderUtils.skipFighterMovementBlock(reader);
        byte lifeState = reader.getByte();
        byte debuffStatus = reader.getByte();
        byte motionState = reader.getByte();
        byte characterStatus = reader.getByte();
        float walkSpeed = reader.getFloat();
        float runSpeed = reader.getFloat();
        reader.getFloat(); // hwanSpeed

        int activeBuffCount = reader.getByte() & 0xFF;
        for (int i = 0; i < activeBuffCount; i++) {
            int buffRefId = reader.getInt();
            int duration = reader.getInt();
            if (handlers != null) {
                handlers.onBuff(buffRefId, duration);
            }
        }

        String characterName = PacketReaderUtils.readString(reader);
        String jobName = PacketReaderUtils.readString(reader);
        byte jobType = reader.getByte();
        byte jobLevel = reader.getByte();
        int jobExp = reader.getInt();
        reader.getInt(); // jobContribution
        reader.getInt(); // jobReward
        byte pvpState = reader.getByte();
        boolean hasTransport = reader.getBoolean();
        boolean inCombat = reader.getBoolean();
        if (hasTransport) {
            reader.getInt(); // transportId
        }
        reader.getByte(); // pvpFlag
        reader.getLong(); // guideFlag
        reader.getInt(); // accountId
        reader.getByte(); // gmFlag
        reader.getByte(); // activationFlag

        int hotKeyCount = reader.getByte() & 0xFF;
        for (int i = 0; i < hotKeyCount; i++) {
            reader.getByte();
            reader.getByte();
            reader.getInt();
        }

        return new CharacterData(
                refId, level, maxLevel, experience, gold, skillPoints, statPoints,
                currentHP, currentMP, lifeState, debuffStatus, motionState, characterStatus,
                walkSpeed, runSpeed, uniqueId, xSector, ySector, xOffset, yOffset, zOffset, angle,
                itemCount, avatarItemCount, masteryCount, skillCount, activeBuffCount, hotKeyCount,
                characterName, jobName, jobType, jobLevel, jobExp, pvpState, hasTransport, inCombat);
    }
}

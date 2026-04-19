package org.sokybot.gameevents.script;

import org.sokybot.network.packet.IStreamReader;

/**
 * Static utilities for reading common packet structures.
 * Extracted from duplicated logic in CharacterLoadedTranslator, CharacterDataEndTranslator,
 * and SpawnDataReader.
 */
public final class PacketReaderUtils {

    private PacketReaderUtils() {}

    /**
     * Read a length-prefixed string (short length, then bytes).
     */
    public static String readString(IStreamReader reader) {
        int length = reader.getShort() & 0xFFFF;
        return new String(reader.getBytes(length));
    }

    /**
     * Skip one item entry (slot, rentType + rent data, refId).
     */
    public static void skipItem(IStreamReader reader) {
        reader.getByte(); // slot
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
        reader.getInt(); // refId
    }

    /**
     * Reads one inventory-style item stack header (slot byte, rentType, rent payload, refObjId) and returns
     * {@code int[] { slotIndex, itemRefId }}.
     * <p>
     * Used for best-effort storage (0x3049) row parsing; quantity is not part of this common record and is
     * defaulted by the caller.
     * </p>
     */
    public static int[] readInventoryStyleSlotAndRefId(IStreamReader reader) {
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
        int refId = reader.getInt();
        return new int[] { slot & 0xFF, refId };
    }

    /**
     * Skip one quest entry (variable structure by type).
     */
    public static void skipQuest(IStreamReader reader) {
        reader.getInt(); // questId
        reader.getByte(); // achievementCount
        reader.getByte(); // requiresSharePt
        byte type = reader.getByte();
        if (type == 28) {
            reader.getInt();
        }
        reader.getByte(); // status
        if (type != 8) {
            int objCount = reader.getByte() & 0xFF;
            for (int i = 0; i < objCount; i++) {
                reader.getByte();
                reader.getByte();
                int nameLen = reader.getShort() & 0xFFFF;
                reader.getBytes(nameLen);
                int taskCount = reader.getByte() & 0xFF;
                for (int j = 0; j < taskCount; j++) {
                    reader.getInt();
                }
            }
        }
        if (type == 88) {
            int taskCount = reader.getByte() & 0xFF;
            for (int i = 0; i < taskCount; i++) {
                reader.getInt();
            }
        }
    }

    /**
     * Skip one buff entry (buffRefId, duration).
     */
    public static void skipBuff(IStreamReader reader) {
        reader.getInt(); // buffRefId
        reader.getInt(); // duration
    }

    /**
     * Skip the fighter movement/destination block used in character data and spawn packets:
     * hasDestination, movementType, then either (destXSector, destYSector, offsets) or (skyClick, angle).
     */
    public static void skipFighterMovementBlock(IStreamReader reader) {
        boolean hasDest = reader.getBoolean();
        reader.getByte(); // movementType
        if (hasDest) {
            reader.getUnsignedByte(); // destXSector
            int destYSector = reader.getUnsignedByte();
            if (destYSector == 0x80) {
                reader.getShort();
                reader.getShort();
                reader.getShort();
                reader.getShort();
                reader.getShort();
                reader.getShort();
            } else {
                reader.getShort();
                reader.getShort();
                reader.getShort();
            }
        } else {
            reader.getByte(); // skyClickFlag
            reader.getShort(); // angle
        }
    }

    /**
     * Whether a skill longId represents a transferable buff (requires extra boolean in packet).
     */
    public static boolean isTransferableBuff(String longId) {
        if (longId == null) {
            return false;
        }
        return longId.contains("SKILL_EU_CLERIC_RECOVERYA_QUICK_B")
                || longId.contains("SKILL_EU_CLERIC_RECOVERYA_GROUP")
                || longId.contains("BATTLAA_GUARD")
                || longId.contains("BARD_DANCEA")
                || longId.contains("BARD_SPEEDUPA_HITRATE");
    }
}

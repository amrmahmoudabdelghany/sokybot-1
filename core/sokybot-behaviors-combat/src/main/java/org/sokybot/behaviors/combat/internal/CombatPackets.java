package org.sokybot.behaviors.combat.internal;

import org.sokybot.commons.SilkroadUtils;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.events.inventory.InventoryOperationEvent;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;

/**
 * Client opcodes aligned with {@code sokybot-behaviors-training} combat patterns.
 */
public final class CombatPackets {

    /** Inventory manipulation (paired with server {@code 0xB034} inventory operation ack). */
    static final int CLIENT_INVENTORY_OPERATION = 0x7034;

    /** Inventory item use (server announces {@code 0xB04C} ItemUse). */
    static final int CLIENT_ITEM_USE = 0x704C;

    /** Skill cast request (matches {@link org.sokybot.behaviors.training.internal.CombatBehavior}). */
    static final int CLIENT_SKILL_CAST = ClientOpcode.JOIN_REQUEST;

    /** Character movement click / walk ({@link ClientOpcode#CHAR_MOVEMENT}). */
    private static final int CLIENT_CHAR_MOVEMENT = ClientOpcode.CHAR_MOVEMENT;

    private static final byte CHAR_ACTION_ATTACK = 0x01;
    /** Ground item pickup (common client convention; tune per server if needed). */
    private static final byte CHAR_ACTION_PICKUP = 0x02;

    private CombatPackets() {
    }

    /**
     * Move items between inventory/equip slots (inventory op uses {@link InventoryOperationEvent#OP_MOVE_SLOTS}).
     */
    static void sendInventoryMoveSlot(IWorkflowContext ctx, byte sourceSlot, byte destSlot, short quantity) {
        MutablePacket packet = MutablePacket.getBuilder(7, CLIENT_INVENTORY_OPERATION)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put(InventoryOperationEvent.OP_MOVE_SLOTS)
                .put(sourceSlot)
                .put(destSlot)
                .putShort(quantity)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    static void sendInventoryItemUse(IWorkflowContext ctx, byte slot, int itemRefId) {
        MutablePacket packet = MutablePacket.getBuilder(5, CLIENT_ITEM_USE)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put(slot)
                .putInt(itemRefId)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    public static void sendSelectEntity(IWorkflowContext ctx, int uniqueEntityId) {
        MutablePacket packet = MutablePacket.getBuilder(4, ClientOpcode.CHAR_SELECT)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .putInt(uniqueEntityId)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    static void sendCharActionAttack(IWorkflowContext ctx, int targetUniqueId) {
        MutablePacket packet = MutablePacket.getBuilder(7, ClientOpcode.CHAR_ACTION)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put(CHAR_ACTION_ATTACK)
                .put((byte) 0x01)
                .put((byte) 0x01)
                .putInt(targetUniqueId)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    static void sendCharActionPickup(IWorkflowContext ctx, int groundItemEntityId) {
        MutablePacket packet = MutablePacket.getBuilder(7, ClientOpcode.CHAR_ACTION)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put(CHAR_ACTION_PICKUP)
                .put((byte) 0x01)
                .put((byte) 0x01)
                .putInt(groundItemEntityId)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    static void sendSkillCast(IWorkflowContext ctx, int skillRefId, int targetUniqueId) {
        MutablePacket packet = MutablePacket.getBuilder(10, CLIENT_SKILL_CAST)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put((byte) 0x01)
                .put((byte) 0x01)
                .putInt(skillRefId)
                .put((byte) 0x01)
                .putInt(targetUniqueId)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    /**
     * Skill cast at world coordinates: same header as {@link #sendSkillCast} with target id {@code 0}, then movement
     * sector/offset block (mirrors {@link #sendCharMove}).
     */
    static void sendSkillCastAtPoint(IWorkflowContext ctx, int skillRefId, float dstX, float dstY, float dstZ,
            int sectorX, int sectorY) {
        short sx = (short) sectorX;
        short sy = (short) sectorY;
        int xOff = SilkroadUtils.getXOffset((int) dstX, sx);
        int zOff = SilkroadUtils.getSectorOffset(dstZ);
        int yOff = SilkroadUtils.getYOffset((int) dstY, sy);
        MutablePacket packet = MutablePacket.getBuilder(19, CLIENT_SKILL_CAST)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put((byte) 0x01)
                .put((byte) 0x01)
                .putInt(skillRefId)
                .put((byte) 0x01)
                .putInt(0)
                .put((byte) (sectorX & 0xFF))
                .put((byte) (sectorY & 0xFF))
                .putShort(clampToShort(xOff))
                .putShort(clampToShort(zOff))
                .putShort(clampToShort(yOff))
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    /**
     * Client walk / movement to world coordinates (opcode {@link ClientOpcode#CHAR_MOVEMENT}), encoding aligned with
     * server movement 0xB021 destination branch (sector bytes + three short offsets X, Z, Y).
     */
    static void sendCharMove(IWorkflowContext ctx, float dstX, float dstY, float dstZ, int sectorX, int sectorY) {
        short sx = (short) sectorX;
        short sy = (short) sectorY;
        int xOff = SilkroadUtils.getXOffset((int) dstX, sx);
        int zOff = SilkroadUtils.getSectorOffset(dstZ);
        int yOff = SilkroadUtils.getYOffset((int) dstY, sy);
        MutablePacket packet = MutablePacket.getBuilder(8, CLIENT_CHAR_MOVEMENT)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put((byte) (sectorX & 0xFF))
                .put((byte) (sectorY & 0xFF))
                .putShort(clampToShort(xOff))
                .putShort(clampToShort(zOff))
                .putShort(clampToShort(yOff))
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    private static short clampToShort(int v) {
        if (v > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (v < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return (short) v;
    }
}

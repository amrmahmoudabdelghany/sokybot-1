package org.sokybot.behaviors.combat.internal;

import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.events.inventory.InventoryOperationEvent;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;

/**
 * Client opcodes aligned with {@code sokybot-behaviors-training} combat patterns.
 */
final class CombatPackets {

    /** Inventory manipulation (paired with server {@code 0xB034} inventory operation ack). */
    static final int CLIENT_INVENTORY_OPERATION = 0x7034;

    /** Inventory item use (server announces {@code 0xB04C} ItemUse). */
    static final int CLIENT_ITEM_USE = 0x704C;

    /** Skill cast request (matches {@link org.sokybot.behaviors.training.internal.CombatBehavior}). */
    static final int CLIENT_SKILL_CAST = ClientOpcode.JOIN_REQUEST;

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

    static void sendSelectEntity(IWorkflowContext ctx, int uniqueEntityId) {
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
}

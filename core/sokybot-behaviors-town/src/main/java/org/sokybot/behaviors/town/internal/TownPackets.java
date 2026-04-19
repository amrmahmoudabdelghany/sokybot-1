package org.sokybot.behaviors.town.internal;

import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.events.inventory.InventoryOperationEvent;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.town.api.EquipSlot;
import org.sokybot.town.api.NpcRef;

/**
 * Client opcodes for town / vendor flows. Values follow common vSRO / RSBot conventions — tune per shard if needed.
 */
final class TownPackets {

    /** NPC dialog / interaction (paired with server 0xB046 NPC talk). */
    static final int CLIENT_NPC_INTERACT = 0x7046;

    /** Inventory manipulation (paired with server 0xB034 inventory operation ack). */
    static final int CLIENT_INVENTORY_OPERATION = 0x7034;

    private TownPackets() {
    }

    static void sendNpcSelect(IWorkflowContext ctx, NpcRef npc) {
        npc.getEntityUniqueId().ifPresent(uid -> sendCharSelect(ctx, uid.intValue()));
    }

    static void sendCharSelect(IWorkflowContext ctx, int entityUniqueId) {
        MutablePacket packet = MutablePacket.getBuilder(4, ClientOpcode.CHAR_SELECT)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .putInt(entityUniqueId)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    /** Opens dialog / trade panel with standard talk option byte. */
    static void sendNpcInteract(IWorkflowContext ctx, int npcUniqueId, byte talkOption) {
        MutablePacket packet = MutablePacket.getBuilder(5, CLIENT_NPC_INTERACT)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .putInt(npcUniqueId)
                .put(talkOption)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    static void sendBuy(IWorkflowContext ctx, byte tabOrService, int npcUniqueId, int itemRefId, short quantity) {
        MutablePacket packet = MutablePacket.getBuilder(14, CLIENT_INVENTORY_OPERATION)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put((byte) 0x06)
                .put(tabOrService)
                .putInt(npcUniqueId)
                .putInt(itemRefId)
                .putShort(quantity)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    static void sendSell(IWorkflowContext ctx, byte shopSlot, short quantity) {
        MutablePacket packet = MutablePacket.getBuilder(6, CLIENT_INVENTORY_OPERATION)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put((byte) 0x07)
                .put(shopSlot)
                .putShort(quantity)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    static void sendDepositItem(IWorkflowContext ctx, int inventorySlotIndex, int quantity) {
        MutablePacket packet = MutablePacket.getBuilder(8, CLIENT_INVENTORY_OPERATION)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put(InventoryOperationEvent.OP_DEPOSIT_ITEM)
                .put((byte) inventorySlotIndex)
                .putShort((short) Math.min(0xffff, quantity))
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    static void sendRepair(IWorkflowContext ctx, byte equipSlotIndex) {
        MutablePacket packet = MutablePacket.getBuilder(3, CLIENT_INVENTORY_OPERATION)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put((byte) 0x0E)
                .put(equipSlotIndex)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    static byte equipSlotByte(EquipSlot slot) {
        switch (slot) {
            case HEAD:
                return 0;
            case CHEST:
                return 1;
            case SHOULDERS:
                return 2;
            case GLOVES:
                return 3;
            case PANTS:
                return 4;
            case BOOTS:
                return 5;
            case MAIN_HAND:
                return 6;
            case OFF_HAND:
                return 7;
            case NECKLACE:
                return 10;
            case EARRING_LEFT:
                return 9;
            case EARRING_RIGHT:
                return 9;
            case RING_LEFT:
                return 11;
            case RING_RIGHT:
                return 12;
            default:
                return 6;
        }
    }
}

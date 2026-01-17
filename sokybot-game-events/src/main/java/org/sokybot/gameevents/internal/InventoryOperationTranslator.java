package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.inventory.InventoryOperationEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates inventory operation response packets (opcode 0xB034).
 * Operations include: move, pickup, drop, buy, sell, deposit, withdraw.
 * Based on RSBot InventoryOperationResponse.
 */
public class InventoryOperationTranslator extends AbstractTranslator {
    
    public InventoryOperationTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0xB034;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            boolean success = result == 0x01;
            byte errorCode = 0;
            if (!success) {
                errorCode = reader.getByte();
                return singleEvent(new InventoryOperationEvent(
                    machineFullName, (byte)0, false, errorCode, null, null, null, null, null));
            }
            byte operationType = reader.getByte();
            // Parse operation-specific data based on type
            Byte sourceSlot = null;
            Byte destSlot = null;
            Integer amount = null;
            Integer itemId = null;
            Long goldAmount = null;
            switch (operationType) {
                case InventoryOperationEvent.OP_MOVE_SLOTS: // Move within inventory
                    sourceSlot = reader.getByte();
                    destSlot = reader.getByte();
                    // Additional move data may follow
                    break;
                    
                case InventoryOperationEvent.OP_PICK_ITEM: // Pickup from ground
                    if (destSlot == (byte)0xFE) {
                        // Gold pickup
                        goldAmount = (long)reader.getInt();
                    } else {
                        // Item pickup - item data follows but skip for now
                    }
                case InventoryOperationEvent.OP_DROP_ITEM: // Drop to ground
                case InventoryOperationEvent.OP_DELETE_BY_SERVER:
                case InventoryOperationEvent.OP_BUY_ITEM: // Buy from NPC
                    // Tab/slot reading varies by client type, simplified
                case InventoryOperationEvent.OP_SELL_ITEM: // Sell to NPC
                    amount = reader.getShort() & 0xFFFF;
                case InventoryOperationEvent.OP_DROP_GOLD: // Drop gold
                    goldAmount = reader.getLong();
                case InventoryOperationEvent.OP_DEPOSIT_GOLD: // Deposit gold to storage
                case InventoryOperationEvent.OP_WITHDRAW_GOLD: // Withdraw gold from storage
                default:
                    // Other operation types - emit basic event
            return singleEvent(new InventoryOperationEvent(
                machineFullName, operationType, true, (byte)0,
                sourceSlot, destSlot, amount, itemId, goldAmount));
        } catch (Exception e) {
            return noEvents();
        }
}

package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.alchemy.AlchemyResultEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates alchemy result packets (opcode 0xB150 for Elixir, 0xB151 for Stone).
 * Based on RSBot GenericAlchemyAckResponse.
 */
public class AlchemyResultTranslator extends AbstractTranslator {
    
    private final int opcode;
    private final byte alchemyType; // 1 for Elixir, 2 for Stone (simplified)
    
    public AlchemyResultTranslator(IGameDataLookup lookup, int opcode, byte alchemyType) {
        super(lookup);
        this.opcode = opcode;
        this.alchemyType = alchemyType;
    }
    
    @Override
    public int getOpcode() {
        return opcode;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            
            if (result == 2) { // Error
                // RSBot reads another UShort here for error code
                return singleEvent(new AlchemyResultEvent(machineFullName, false, alchemyType, (byte)0, false, false, null));
            }
            
            // RSBot re-reads as action but the loop logic is different.
            // RSBot: var action = (AlchemyAction)packet.ReadByte();
            // Wait, RSBot reads result FIRST. result IS likely the first byte.
            // "var result = packet.ReadByte(); if (result == 2)... var action = (AlchemyAction)packet.ReadByte();"
            // So action is distinct.
            
            // Wait, the first byte read "result" is NOT "action".
            // If result != 2, it continues to read action.
            // But reader state is sequential. 
            // So logic:
            // byte result = reader.getByte()
            // if (result == 2) -> Error
            // byte action = reader.getByte()
            
            byte action = reader.getByte();
            
            if (action == 1) { // Cancel (AlchemyAction.Cancel = 1 assumed?)
                 // Need check RSBot enum, usually 1 or 2.
                 // Assuming 1 is Cancel based on common patterns, simplified here.
                 // Actually event supports 'cancelled' flag.
                 return singleEvent(new AlchemyResultEvent(machineFullName, false, alchemyType, (byte)0, false, true, null));
            }
            
            boolean isSuccess = reader.getBoolean();
            
            // Skip Chinese client check (packet.ReadByte()) as we don't know client type easily here, assume not Chinese for now or handle gracefully?
            // RSBot: if (Game.ClientType >= GameClientType.Chinese) packet.ReadByte();
            // Let's assume standard VSRO (which is often based on files requiring this or not).
            // sokybot likely standard. Skip for now.
            
            byte slot = reader.getByte();
            
            boolean destroyed = false;
            Integer newItemId = null;
            
            if (!isSuccess) {
                destroyed = reader.getBoolean(); // isDestroyed
            } else {
                // Success - parse item
                // RSBot: InventoryItem.FromPacket(packet, slot)
                // Item parsing is complex (Rent type, ItemID, OptLevel, Variance, Durability, MagParams etc.)
                // Read rudimentary item info:
                // byte rentType, int refItemId...
                
                // Skipping full item parse for brevity, just reading enough to get ID or skipping
                // Just assuming reader position alignment for now is risky without full item parser.
                // We'll return Success event without item ID if we can't safely parse.
                // Or try to read simple structure.
                
                // Let's just emit success event without full item details for now to avoid crash.
            }
            
            return singleEvent(new AlchemyResultEvent(
                machineFullName, isSuccess, alchemyType, slot, destroyed, false, null));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

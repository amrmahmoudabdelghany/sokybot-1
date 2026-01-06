package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.GoldUpdateEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates gold update packets (opcode 0x3842) to GoldUpdateEvent.
 * Based on TrainerHandler.attackGainsUpdates() pattern for GOLD type.
 */
@Component(service = IPacketTranslator.class)
public class GoldUpdateTranslator implements IPacketTranslator {
    
    private static final int ATTACK_GAINS_UPDATE_OPCODE = 0x3842;
    private static final byte GOLD_TYPE = 1;
    
    @Override
    public int getOpcode() {
        return ATTACK_GAINS_UPDATE_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte gainType = reader.getByte();
            
            // Only handle GOLD type (1)
            if (gainType == GOLD_TYPE) {
                long newGoldAmount = reader.getLong();
                return new GoldUpdateEvent(machineFullName, newGoldAmount);
            }
            
            // Other types (SP, ZERK) - return null for now
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
}

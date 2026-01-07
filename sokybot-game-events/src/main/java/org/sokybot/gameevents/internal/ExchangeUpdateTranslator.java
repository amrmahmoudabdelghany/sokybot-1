package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.ExchangeUpdateEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates exchange update packets (opcode 0x3089) to ExchangeUpdateEvent.
 * Reference: go-sro-framework ExchangeUpdateResponse = 0x3089
 */
public class ExchangeUpdateTranslator extends AbstractTranslator {
    
    private static final int EXCHANGE_UPDATE_OPCODE = 0x3089;
    
    public ExchangeUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return EXCHANGE_UPDATE_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read exchange ID
            int exchangeId = reader.getInt();
            
            // Read who updated (0 = self, 1 = partner)
            byte updatedBy = reader.getByte();
            boolean selfUpdate = updatedBy == 0;
            
            // Read slot
            byte slot = reader.getByte();
            
            // Read item info
            int itemRefId = reader.getInt();
            int quantity = reader.getShort() & 0xFFFF;
            
            // Read gold amount
            long goldAmount = reader.getLong();
            
            return singleEvent(new ExchangeUpdateEvent(machineFullName, exchangeId, selfUpdate,
                                                       slot, itemRefId, quantity, goldAmount));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

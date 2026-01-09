package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.exchange.ExchangeConfirmedEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates exchange confirmed packets (opcode 0x3086) to ExchangeConfirmedEvent.
 * Reference: go-sro-framework ExchangeConfirmedResponse = 0x3086
 */
public class ExchangeConfirmedTranslator extends AbstractTranslator {
    
    private static final int EXCHANGE_CONFIRMED_OPCODE = 0x3086;
    
    public ExchangeConfirmedTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return EXCHANGE_CONFIRMED_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read exchange ID
            int exchangeId = reader.getInt();
            
            // Read who confirmed (0 = self, 1 = partner)
            byte confirmedBy = reader.getByte();
            boolean selfConfirmed = confirmedBy == 0;
            
            return singleEvent(new ExchangeConfirmedEvent(machineFullName, exchangeId, selfConfirmed));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.exchange.ExchangeApprovedEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates exchange approved packets (opcode 0x3087) to ExchangeApprovedEvent.
 * Reference: go-sro-framework ExchangeApprovedResponse = 0x3087
 */
public class ExchangeApprovedTranslator extends AbstractTranslator {
    
    private static final int EXCHANGE_APPROVED_OPCODE = 0x3087;
    public ExchangeApprovedTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return EXCHANGE_APPROVED_OPCODE;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read exchange ID
            int exchangeId = reader.getInt();
            // Read success flag
            byte result = reader.getByte();
            boolean success = result == 0x01;
            return singleEvent(new ExchangeApprovedEvent(machineFullName, exchangeId, success));
        } catch (Exception e) {
            return noEvents();
        }
}

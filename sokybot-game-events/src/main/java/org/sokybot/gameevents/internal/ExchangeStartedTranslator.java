package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.exchange.ExchangeStartedEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates exchange started packets (opcode 0x3085).
 * Based on RSBot ExchangeStartedResponse.
 */
public class ExchangeStartedTranslator extends AbstractTranslator {
    
    public ExchangeStartedTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0x3085;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            int otherPlayerUniqueId = reader.getInt();
            return singleEvent(new ExchangeStartedEvent(machineFullName, otherPlayerUniqueId));
        } catch (Exception e) {
            return noEvents();
        }
}

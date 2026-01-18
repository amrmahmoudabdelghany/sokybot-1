package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.exchange.ExchangeCancelledEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates exchange cancelled packets (opcode 0x3088).
 * Based on RSBot ExchangeCanceledResponse.
 */
public class ExchangeCancelledTranslator extends AbstractTranslator {
    
    public ExchangeCancelledTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0x3088;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        return singleEvent(new ExchangeCancelledEvent(machineFullName));
}
}

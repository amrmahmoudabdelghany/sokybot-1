package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.world.CelestialPositionEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates celestial position packets (opcode 0x3020) to CelestialPositionEvent.
 * Reference: go-sro-framework CelestialPosition = 0x3020
 */
public class CelestialPositionTranslator extends AbstractTranslator {
    
    private static final int CELESTIAL_OPCODE = 0x3020;
    public CelestialPositionTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return CELESTIAL_OPCODE;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int uniqueId = reader.getInt();
            short angle = reader.getShort();
            return singleEvent(new CelestialPositionEvent(machineFullName, uniqueId, angle));
        } catch (Exception e) {
            return noEvents();
        }
}
}

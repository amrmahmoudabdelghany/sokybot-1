package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.world.CelestialUpdateEvent;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates celestial update packets (opcode 0x3027).
 */
public class CelestialUpdateTranslator extends AbstractTranslator {
    public CelestialUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public CelestialUpdateTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0x3027;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            int moonphase = reader.getShort() & 0xFFFF;
            int hour = reader.getByte() & 0xFF;
            int minute = reader.getByte() & 0xFF;

            return singleEvent(new CelestialUpdateEvent(machineFullName, moonphase, hour, minute));
        } catch (Exception e) {
            return noEvents();
        }
    }
}

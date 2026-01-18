package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.world.WeatherUpdateEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates weather update packets (opcode 0x3809) to WeatherUpdateEvent.
 * Reference: go-sro-framework WeatherUpdate = 0x3809
 */
public class WeatherUpdateTranslator extends AbstractTranslator {
    
    private static final int WEATHER_OPCODE = 0x3809;
    public WeatherUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return WEATHER_OPCODE;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte weatherType = reader.getByte();
            int intensity = reader.getUnsignedByte();
            return singleEvent(new WeatherUpdateEvent(machineFullName, weatherType, intensity));
        } catch (Exception e) {
            return noEvents();
        }
}
}

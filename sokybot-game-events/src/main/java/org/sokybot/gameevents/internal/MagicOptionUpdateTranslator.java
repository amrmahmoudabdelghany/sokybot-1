package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.alchemy.MagicOptionUpdateEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates magic option update packets (opcode 0x34AA) to MagicOptionUpdateEvent.
 * Reference: RSBot MagicOptionUpdateResponse = 0x34AA
 */
public class MagicOptionUpdateTranslator extends AbstractTranslator {
    
    private static final int MAGIC_OPTION_OPCODE = 0x34AA;
    
    public MagicOptionUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return MAGIC_OPTION_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            
            if (result == 2) {
                // Error
                int errorCode = reader.getShort() & 0xFFFF;
                return singleEvent(MagicOptionUpdateEvent.failure(machineFullName, errorCode));
            }
            
            byte counter = reader.getByte();
            if (counter != 0) {
                byte slot = reader.getByte();
                // Item data follows but we just emit basic event
                return singleEvent(MagicOptionUpdateEvent.success(machineFullName, slot));
            }
            
            return noEvents();
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

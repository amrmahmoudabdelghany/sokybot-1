package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.LifeStateUpdateEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates entity state update packets (opcode 0x30BF).
 * Emits LifeStateUpdateEvent for life/motion/body/pvp/battle state changes.
 * Based on RSBot EntityUpdateStateResponse.
 */
public class LifeStateUpdateTranslator extends AbstractTranslator {
    
    public LifeStateUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return 0x30BF;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            int uniqueId = reader.getInt();
            byte stateType = reader.getByte();
            byte stateValue = reader.getByte();
            
            return singleEvent(new LifeStateUpdateEvent(
                machineFullName, uniqueId, stateType, stateValue));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

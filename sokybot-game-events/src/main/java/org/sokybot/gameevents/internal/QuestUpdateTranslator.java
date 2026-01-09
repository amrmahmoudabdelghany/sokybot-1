package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.quest.QuestUpdateEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates quest update packets (opcode 0x30D5).
 * Handles: Add, Remove, Abandon, Update quest states.
 * Based on RSBot QuestUpdateResponse.
 */
public class QuestUpdateTranslator extends AbstractTranslator {
    
    public QuestUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return 0x30D5;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            byte updateType = reader.getByte();
            int questId = reader.getInt();
            
            // Quest-specific data follows for Add/Update types but skip for now
            
            return singleEvent(new QuestUpdateEvent(machineFullName, updateType, questId));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

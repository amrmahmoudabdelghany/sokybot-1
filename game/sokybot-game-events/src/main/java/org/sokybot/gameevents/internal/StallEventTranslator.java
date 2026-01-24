package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.stall.StallEvent;
import org.sokybot.gameevents.events.stall.StallEvent.StallEventType;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates stall (street vendor) entity event packets.
 * Handles multiple opcodes: 0x30B7 (action), 0x30B8 (create), 0x30B9 (destroy), 0x30BB (name).
 * Reference: go-sro-framework StallEntity* opcodes
 */
public class StallEventTranslator extends AbstractTranslator {
    
    private final int opcode;
    private final StallEventType eventType;

    public StallEventTranslator(IGameDataLookup lookup, int opcode, StallEventType eventType) {
        super(lookup);
        this.opcode = opcode;
        this.eventType = eventType;
    }

    public static StallEventTranslator forAction(IGameDataLookup lookup) {
        return new StallEventTranslator(lookup, 0x30B7, StallEventType.ACTION);
    }

    public static StallEventTranslator forCreated(IGameDataLookup lookup) {
        return new StallEventTranslator(lookup, 0x30B8, StallEventType.CREATED);
    }

    public static StallEventTranslator forDestroyed(IGameDataLookup lookup) {
        return new StallEventTranslator(lookup, 0x30B9, StallEventType.DESTROYED);
    }

    public static StallEventTranslator forNameChanged(IGameDataLookup lookup) {
        return new StallEventTranslator(lookup, 0x30BB, StallEventType.NAME_CHANGED);
    }

    @Override
    public int getOpcode() {
        return opcode;
    }

    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read entity ID
            int entityId = reader.getInt();
            String stallName = null;
            if (eventType == StallEventType.NAME_CHANGED) {
                // Read stall name (unicode string)
                int nameLen = reader.getShort() & 0xFFFF;
                stallName = reader.getUnicodeString(nameLen);
            }
            return singleEvent(new StallEvent(machineFullName, entityId, eventType, stallName));
        } catch (Exception e) {
            return noEvents();
        }
    }
}

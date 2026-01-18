package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.stat.HwanLevelUpdateEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates Hwan level update packets (opcode 0x30DF) to HwanLevelUpdateEvent.
 * Hwan is a buff system that provides bonuses at different levels.
 * Reference: go-sro-framework EntityUpdateHwanLevel = 0x30DF
 */
public class HwanLevelUpdateTranslator extends AbstractTranslator {
    
    private static final int HWAN_UPDATE_OPCODE = 0x30DF;
    public HwanLevelUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return HWAN_UPDATE_OPCODE;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read entity ID
            int entityId = reader.getInt();
            // Read Hwan level (0-5)
            byte hwanLevel = reader.getByte();
            // Read Hwan progress toward next level
            int hwanProgress = reader.getInt();
            return singleEvent(new HwanLevelUpdateEvent(machineFullName, entityId, hwanLevel, hwanProgress));
        } catch (Exception e) {
            return noEvents();
        }
}
}

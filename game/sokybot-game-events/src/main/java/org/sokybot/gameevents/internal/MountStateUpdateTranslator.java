package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.combat.MountStateUpdateEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates mount state update packets (opcode 0xB0CB).
 * Based on RSBot UpdateMountStateResponse.
 */
public class MountStateUpdateTranslator extends AbstractTranslator {
    
    public MountStateUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0xB0CB;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            if (result != 0x01) {
                return noEvents();
            }
            int ownerUniqueId = reader.getInt();
            boolean mounted = reader.getByte() != 0;
            int petUniqueId = reader.getInt();
            return singleEvent(new MountStateUpdateEvent(
                machineFullName, ownerUniqueId, mounted, petUniqueId));
        } catch (Exception e) {
            return noEvents();
        }
}
}

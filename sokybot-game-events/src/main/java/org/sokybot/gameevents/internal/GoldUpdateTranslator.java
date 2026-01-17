package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.stat.GoldUpdateEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates gold update packets (opcode 0x304E with type GOLD) to GoldUpdateEvent.
 * Based on TrainerHandler.attackGainsUpdates() pattern for GOLD type.
 */
public class GoldUpdateTranslator extends AbstractTranslator {
    
    private static final int ATTACK_GAINS_UPDATE_OPCODE = 0x304E;
    private static final byte GOLD_TYPE = 1;
    public GoldUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return ATTACK_GAINS_UPDATE_OPCODE;
    }
    
    @Override
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte gainType = reader.getByte();
            // Only handle GOLD type (1)
            if (gainType == GOLD_TYPE) {
                long newGoldAmount = reader.getLong();
                return singleEvent(new GoldUpdateEvent(machineFullName, newGoldAmount));
            }
            // Other types (SP, ZERK) - return empty for now
            return noEvents();
        } catch (Exception e) {
            return noEvents();
        }
    }
}

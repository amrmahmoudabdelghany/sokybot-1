package org.sokybot.gameevents.internal;

import org.sokybot.api.events.BerserkConfirmEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates berserk confirm packets (opcode 0xB0A7) to BerserkConfirmEvent.
 * Based on ServerOpcode.BESERK_CONFIRM definition.
 */
public class BerserkConfirmTranslator extends AbstractTranslator {
    
    private static final int BESERK_CONFIRM_OPCODE = 0xB0A7;
    
    public BerserkConfirmTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return BESERK_CONFIRM_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            boolean success = reader.getBoolean();
            byte berserkLevel = 0;
            
            if (success) {
                berserkLevel = reader.getByte();
            }
            
            return new BerserkConfirmEvent(machineFullName, success, berserkLevel);
            
        } catch (Exception e) {
            return null;
        }
    }
}

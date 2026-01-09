package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.session.LoginResponseEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates login response packets (opcode 0xA102) to LoginResponseEvent.
 * Based on ServerOpcode.LOGIN_RESPONSE - received after login attempt.
 */
public class LoginResponseTranslator extends AbstractTranslator {
    
    private static final int LOGIN_RESPONSE_OPCODE = 0xA102;
    
    public LoginResponseTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return LOGIN_RESPONSE_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte resultCode = reader.getByte();
            boolean success = (resultCode == 1);
            
            return singleEvent(new LoginResponseEvent(machineFullName, success, resultCode));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

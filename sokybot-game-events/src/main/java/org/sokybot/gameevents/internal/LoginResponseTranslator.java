package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.api.events.LoginResponseEvent;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates login response packets (opcode 0xA102) to LoginResponseEvent.
 * Based on ServerOpcode.LOGIN_RESPONSE - received after login attempt.
 */
@Component(service = IPacketTranslator.class)
public class LoginResponseTranslator implements IPacketTranslator {
    
    private static final int LOGIN_RESPONSE_OPCODE = 0xA102;
    
    @Override
    public int getOpcode() {
        return LOGIN_RESPONSE_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte resultCode = reader.getByte();
            boolean success = (resultCode == 1);
            
            return new LoginResponseEvent(machineFullName, success, resultCode);
            
        } catch (Exception e) {
            return null;
        }
    }
}

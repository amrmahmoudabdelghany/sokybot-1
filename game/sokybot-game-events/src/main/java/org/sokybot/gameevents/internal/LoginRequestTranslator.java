package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.session.LoginRequestEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
public class LoginRequestTranslator extends AbstractTranslator {
    
    // ClientOpcode.LOGIN_REQUEST = 0x6102
    private static final int LOGIN_REQUEST_OPCODE = 0x6102;
    public LoginRequestTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return LOGIN_REQUEST_OPCODE;
    }
    
    @Override
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            reader.skip(1); // local
            String username = new String(reader.getBytes(reader.getShort()));
            String password = new String(reader.getBytes(reader.getShort()));
            return singleEvent(new LoginRequestEvent(machineFullName, username, password));
        } catch (Exception e) {
            return noEvents();
        }
    }
}

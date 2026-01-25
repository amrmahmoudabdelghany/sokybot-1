package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.character.CharacterRenameAckEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates character/guild rename response (opcode 0xB450).
 */
public class CharacterRenameAckTranslator extends AbstractTranslator {
    public CharacterRenameAckTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public CharacterRenameAckTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0xB450;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            byte renameAction = reader.getByte();
            byte result = reader.getByte();
            int errorCode = 0;
            if (result == 2) {
                errorCode = reader.getShort() & 0xFFFF;
            }

            return singleEvent(new CharacterRenameAckEvent(machineFullName, renameAction, result, errorCode));
        } catch (Exception e) {
            return noEvents();
        }
    }
}

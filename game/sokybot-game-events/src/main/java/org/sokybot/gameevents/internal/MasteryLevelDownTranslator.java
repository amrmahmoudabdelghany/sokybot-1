package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.skill.MasteryLevelDownEvent;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates mastery level down response (opcode 0xB203).
 */
public class MasteryLevelDownTranslator extends AbstractTranslator {
    public MasteryLevelDownTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public MasteryLevelDownTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0xB203;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            boolean success = reader.getByte() == 1;
            int errorCode = 0;
            if (!success) {
                errorCode = reader.getShort() & 0xFFFF;
            }

            return singleEvent(new MasteryLevelDownEvent(machineFullName, success, errorCode));
        } catch (Exception e) {
            return noEvents();
        }
    }
}

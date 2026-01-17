package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.quest.QuestAbandonEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates quest abandon response packets (opcode 0xB0D9) to QuestAbandonEvent.
 * Reference: RSBot QuestAbandonResponse = 0xB0D9
 */
public class QuestAbandonTranslator extends AbstractTranslator {
    
    private static final int QUEST_ABANDON_OPCODE = 0xB0D9;
    public QuestAbandonTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return QUEST_ABANDON_OPCODE;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            boolean success = result == 0x01;
            int questId = 0;
            if (success) {
                questId = reader.getInt();
            }
            return singleEvent(new QuestAbandonEvent(machineFullName, success, questId));
        } catch (Exception e) {
            return noEvents();
        }
}

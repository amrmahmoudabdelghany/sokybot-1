package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.chat.NpcTalkEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates NPC talk response packets (opcode 0xB046).
 * Based on RSBot ActionTalkResponse.
 */
public class NpcTalkTranslator extends AbstractTranslator {
    
    public NpcTalkTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0xB046;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            if (result != 0x01) {
                return noEvents();
            }
            byte talkOption = reader.getByte();
            // NPC unique ID would need to be tracked from request, use 0 for now
            int npcUniqueId = 0;
            return singleEvent(new NpcTalkEvent(machineFullName, talkOption, npcUniqueId));
        } catch (Exception e) {
            return noEvents();
        }
}

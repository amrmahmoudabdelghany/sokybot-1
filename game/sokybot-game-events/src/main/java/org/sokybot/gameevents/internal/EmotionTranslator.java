package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.chat.EmotionEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates entity emotion/emote packets (opcode 0x3091) to EmotionEvent.
 * Reference: go-sro-framework EntityEmotion = 0x3091
 */
public class EmotionTranslator extends AbstractTranslator {
    
    private static final int EMOTION_OPCODE = 0x3091;
    public EmotionTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return EMOTION_OPCODE;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read entity performing the emotion
            int entityId = reader.getInt();
            // Read emotion ID (emote type)
            int emotionId = reader.getUnsignedByte();
            return singleEvent(new EmotionEvent(machineFullName, entityId, emotionId));
        } catch (Exception e) {
            return noEvents();
        }
}
}

package org.sokybot.gameevents.internal;

import java.util.ArrayList;
import java.util.List;
import org.sokybot.gameevents.events.session.GameReadyEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates game ready/buff token packets (opcode 0x3077) to GameReadyEvent.
 * Reference: RSBot BuffTokenUpdateResponse = 0x3077
 */
public class GameReadyTranslator extends AbstractTranslator {
    
    private static final int GAME_READY_OPCODE = 0x3077;
    public GameReadyTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return GAME_READY_OPCODE;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read item cooldowns
            List<GameReadyEvent.CooldownInfo> itemCooldowns = new ArrayList<>();
            int itemCount = reader.getUnsignedByte();
            for (int i = 0; i < itemCount; i++) {
                int itemId = reader.getInt();
                int milliseconds = reader.getInt();
                itemCooldowns.add(new GameReadyEvent.CooldownInfo(itemId, milliseconds));
            }
            // Read skill cooldowns
            List<GameReadyEvent.CooldownInfo> skillCooldowns = new ArrayList<>();
            int skillCount = reader.getUnsignedByte();
            for (int i = 0; i < skillCount; i++) {
                int skillId = reader.getInt();
                skillCooldowns.add(new GameReadyEvent.CooldownInfo(skillId, milliseconds));
            return singleEvent(new GameReadyEvent(machineFullName, itemCooldowns, skillCooldowns));
        } catch (Exception e) {
            return noEvents();
        }
}

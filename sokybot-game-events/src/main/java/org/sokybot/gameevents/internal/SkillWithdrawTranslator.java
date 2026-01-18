package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.skill.SkillWithdrawEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates skill withdraw response packets (opcode 0xB202) to SkillWithdrawEvent.
 * Reference: RSBot SkillWithdrawResponse = 0xB202
 */
public class SkillWithdrawTranslator extends AbstractTranslator {
    
    private static final int SKILL_WITHDRAW_OPCODE = 0xB202;
    public SkillWithdrawTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return SKILL_WITHDRAW_OPCODE;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            boolean success = result == 0x01;
            int newSkillId = 0;
            if (success) {
                newSkillId = reader.getInt();
            }
            // Note: oldSkillId is tracked by the application, not in this packet
            return singleEvent(new SkillWithdrawEvent(machineFullName, success, 0, newSkillId));
        } catch (Exception e) {
            return noEvents();
        }
}
}

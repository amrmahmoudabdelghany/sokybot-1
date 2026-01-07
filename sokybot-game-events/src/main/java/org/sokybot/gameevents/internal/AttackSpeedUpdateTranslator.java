package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.AttackSpeedUpdateEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates attack speed update packets (opcode 0x3200) to AttackSpeedUpdateEvent.
 * Reference: go-sro-framework EntityUpdateAttackSpeed = 0x3200
 */
public class AttackSpeedUpdateTranslator extends AbstractTranslator {
    
    private static final int ATTACK_SPEED_OPCODE = 0x3200;
    
    public AttackSpeedUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return ATTACK_SPEED_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read entity ID
            int entityId = reader.getInt();
            
            // Read attack speed value
            int attackSpeed = reader.getInt();
            
            return singleEvent(new AttackSpeedUpdateEvent(machineFullName, entityId, attackSpeed));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.buff.DamageEffectEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates damage effect packets (opcode 0x3058) to DamageEffectEvent.
 * Shows damage numbers floating above entities during combat.
 * Reference: go-sro-framework EntityDamageEffect = 0x3058
 */
public class DamageEffectTranslator extends AbstractTranslator {
    
    private static final int DAMAGE_EFFECT_OPCODE = 0x3058;
    
    public DamageEffectTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return DAMAGE_EFFECT_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read target entity ID
            int targetEntityId = reader.getInt();
            
            // Read damage type
            byte damageType = reader.getByte();
            
            // Read damage amount
            int damageAmount = reader.getInt();
            
            return singleEvent(new DamageEffectEvent(machineFullName, targetEntityId, damageAmount, damageType));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

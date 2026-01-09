package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.combat.PickupAnimationEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates pickup animation packets (opcode 0x3036) to PickupAnimationEvent.
 * Reference: go-sro-framework EntityAnimationPickup = 0x3036
 */
public class PickupAnimationTranslator extends AbstractTranslator {
    
    private static final int PICKUP_ANIMATION_OPCODE = 0x3036;
    
    public PickupAnimationTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return PICKUP_ANIMATION_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read entity performing the pickup
            int entityId = reader.getInt();
            
            // Read target item unique ID
            int targetItemId = reader.getInt();
            
            return singleEvent(new PickupAnimationEvent(machineFullName, entityId, targetItemId));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

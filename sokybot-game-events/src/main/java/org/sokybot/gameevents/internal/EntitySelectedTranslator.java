package org.sokybot.gameevents.internal;

import org.sokybot.api.events.EntitySelectedEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates entity selected packets (opcode 0x7045) to EntitySelectedEvent.
 * Based on EnvironmentHandler.onSpawnSelected() pattern.
 */
public class EntitySelectedTranslator extends AbstractTranslator {
    
    private static final int SPAWN_SELECTED_OPCODE = 0x7045;
    
    public EntitySelectedTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return SPAWN_SELECTED_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            boolean hasSelection = reader.getBoolean();
            
            if (hasSelection) {
                int selectedEntityId = reader.getInt();
                Integer currentHP = null;
                
                // Check if HP info is included
                boolean hasHPInfo = reader.getBoolean();
                if (hasHPInfo) {
                    currentHP = reader.getInt();
                }
                
                return new EntitySelectedEvent(machineFullName, selectedEntityId, currentHP);
            }
            
            // No selection - entity deselected (could return null or a special event)
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
}

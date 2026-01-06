package org.sokybot.gameevents.internal;

import org.sokybot.api.events.EntityMovementEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.entities.navmesh.Position;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.utils.SilkroadUtils;

/**
 * Translates spawn movement packets (opcode 0x3020) to EntityMovementEvent.
 * Based on EnvironmentHandler.onSpawnMove() pattern.
 */
public class EntityMovementTranslator extends AbstractTranslator {
    
    private static final int SPAWN_MOVEMENT_OPCODE = 0x3020;
    
    public EntityMovementTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return SPAWN_MOVEMENT_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int entityId = reader.getInt();
            boolean hasDestination = reader.getBoolean();
            
            Position destination = null;
            Position currentPosition = null;
            
            if (hasDestination) {
                // Read destination
                int destXSector = reader.getUnsignedByte();
                int destYSector = reader.getUnsignedByte();
                
                // Check if in cave (sector == 0x80)
                boolean inCave = (destYSector == 0x80);
                
                float destXOffset, destZOffset, destYOffset;
                if (inCave) {
                    destXOffset = reader.getInt();
                    destZOffset = reader.getInt();
                    destYOffset = reader.getInt();
                } else {
                    destXOffset = reader.getShort();
                    destZOffset = reader.getShort();
                    destYOffset = reader.getShort();
                }
                
                destination = new Position(destXOffset, destYOffset, destZOffset);
            } else {
                // No destination - read sky click flag and angle
                reader.getByte(); // skyClickFlag
                reader.getByte(); // angleAction
            }
            
            // Check if has origin position
            boolean hasOrigin = reader.getBoolean();
            if (hasOrigin) {
                int xSector = reader.getUnsignedByte();
                int ySector = reader.getUnsignedByte();
                float xOffset = reader.getShort();
                float zOffset = reader.getShort();
                reader.getShort(); // angle
                float yOffset = reader.getShort();
                
                currentPosition = new Position(xOffset, yOffset, zOffset);
            }
            
            return new EntityMovementEvent(machineFullName, entityId, hasDestination, 
                                          destination, currentPosition);
            
        } catch (Exception e) {
            return null;
        }
    }
}

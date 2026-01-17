package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.entity.EntityMovementEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.entities.navmesh.Position;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.commons.SilkroadUtils;
/**
 * Translates spawn movement packets (opcode 0xB021) to EntityMovementEvent.
 * Based on EnvironmentHandler.onSpawnMove() pattern.
 */
public class EntityMovementTranslator extends AbstractTranslator {
    
    private static final int SPAWN_MOVEMENT_OPCODE = 0xB021;
    public EntityMovementTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return SPAWN_MOVEMENT_OPCODE;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Packet structure (based on EnvironmentHandler.onSpawnMove):
            // 1. entityId (int)
            // 2. hasDestination (boolean)
            // 3a. If hasDestination: destXSector, destYSector, offsets
            // 3b. If !hasDestination: skyClickFlag, angleAction
            // 4. hasOrigin (boolean)
            // 5. If hasOrigin: xSector, ySector, offsets, angle
            // NOTE: MovementType is NOT in this packet - it's only read during spawn
            int entityId = reader.getInt();
            boolean hasDestination = reader.getBoolean();
            Byte movementType = null;  // Not in this packet type
            Position destination = null;
            Integer destXSector = null;
            Integer destYSector = null;
            Byte skyClickFlag = null;
            Byte angleAction = null;
            if (hasDestination) {
                // Read destination with sectors
                int destXSectorVal = reader.getUnsignedByte();
                int destYSectorVal = reader.getUnsignedByte();
                
                // Check if in cave (sector == 0x80)
                boolean inCave = (destYSectorVal == 0x80);
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
                destXSector = destXSectorVal;
                destYSector = destYSectorVal;
            } else {
                // No destination - read sky click flag and angle action
                skyClickFlag = reader.getByte();
                angleAction = reader.getByte();
            }
            // Check if has origin position
            Position currentPosition = null;
            Integer currentXSector = null;
            Integer currentYSector = null;
            Short currentAngle = null;
            boolean hasOrigin = reader.getBoolean();
            if (hasOrigin) {
                int xSector = reader.getUnsignedByte();
                int ySector = reader.getUnsignedByte();
                float xOffset = reader.getShort();
                float zOffset = reader.getShort();
                short angle = reader.getShort();
                float yOffset = reader.getShort();
                currentPosition = new Position(xOffset, yOffset, zOffset);
                currentXSector = xSector;
                currentYSector = ySector;
                currentAngle = angle;
            return singleEvent(new EntityMovementEvent(machineFullName, entityId, hasDestination, 
                                          destination, destXSector, destYSector,
                                          currentPosition, currentXSector, currentYSector,
                                          currentAngle, movementType, skyClickFlag, angleAction));
        } catch (Exception e) {
            return noEvents();
        }
}

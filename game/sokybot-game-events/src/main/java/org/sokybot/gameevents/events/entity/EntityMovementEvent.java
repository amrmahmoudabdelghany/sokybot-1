package org.sokybot.gameevents.events.entity;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.dto.GamePosition;

/**
 * Event fired when an entity moves in the game world.
 * Contains movement destination and optionally current position.
 * Enhanced to include sector information for proper coordinate calculations.
 */
public class EntityMovementEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final int entityId;
    private final boolean hasDestination;

    // Destination coordinates with sectors
    private final GamePosition destination;
    private final Integer destXSector;
    private final Integer destYSector;

    // Current position with sectors (if hasOrigin flag is set)
    private final GamePosition currentPosition;
    private final Integer currentXSector;
    private final Integer currentYSector;
    private final Short currentAngle; // Angle when hasOrigin is true

    // Additional movement data
    private final Byte movementType;
    private final Byte skyClickFlag; // When !hasDestination
    private final Byte angleAction; // When !hasDestination

    public EntityMovementEvent(String machineFullName, int entityId, boolean hasDestination,
            GamePosition destination, Integer destXSector, Integer destYSector,
            GamePosition currentPosition, Integer currentXSector, Integer currentYSector,
            Short currentAngle, Byte movementType, Byte skyClickFlag, Byte angleAction) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
        this.hasDestination = hasDestination;
        this.destination = destination;
        this.destXSector = destXSector;
        this.destYSector = destYSector;
        this.currentPosition = currentPosition;
        this.currentXSector = currentXSector;
        this.currentYSector = currentYSector;
        this.currentAngle = currentAngle;
        this.movementType = movementType;
        this.skyClickFlag = skyClickFlag;
        this.angleAction = angleAction;
    }

    // Legacy constructor for backward compatibility
    public EntityMovementEvent(String machineFullName, int entityId, boolean hasDestination,
            GamePosition destination, GamePosition currentPosition) {
        this(machineFullName, entityId, hasDestination, destination, null, null,
                currentPosition, null, null, null, null, null, null);
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public String getGroupName() {
        return fullName.split("\\.")[0];
    }

    @Override
    public String getMachineName() {
        return fullName.split("\\.")[1];
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public int getEntityId() {
        return entityId;
    }

    public boolean hasDestination() {
        return hasDestination;
    }

    public GamePosition getDestination() {
        return destination;
    }

    public GamePosition getCurrentPosition() {
        return currentPosition;
    }

    public Integer getDestXSector() {
        return destXSector;
    }

    public Integer getDestYSector() {
        return destYSector;
    }

    public Integer getCurrentXSector() {
        return currentXSector;
    }

    public Integer getCurrentYSector() {
        return currentYSector;
    }

    public Short getCurrentAngle() {
        return currentAngle;
    }

    public Byte getMovementType() {
        return movementType;
    }

    public Byte getSkyClickFlag() {
        return skyClickFlag;
    }

    public Byte getAngleAction() {
        return angleAction;
    }
}

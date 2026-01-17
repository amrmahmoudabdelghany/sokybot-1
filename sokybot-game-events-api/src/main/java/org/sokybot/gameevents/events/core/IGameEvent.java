package org.sokybot.gameevents.events.core;

/**
 * Base interface for all game domain events.
 * Events are the contracts between modules in the event-driven architecture.
 */
public interface IGameEvent {
    
    /**
     * Gets the full machine name that this event originated from.
     * Format: "groupName.machineName" (e.g., "LegionSRO.Machine001")
     */
    String getFullName();
    
    /**
     * Gets the group name extracted from fullName.
     */
    default String getGroupName() {
        String fullName = getFullName();
        int dotIndex = fullName.indexOf('.');
        return dotIndex >= 0 ? fullName.substring(0, dotIndex) : fullName;
    }
    
    /**
     * Gets the machine name extracted from fullName.
     */
    default String getMachineName() {
        String fullName = getFullName();
        int dotIndex = fullName.indexOf('.');
        return dotIndex >= 0 ? fullName.substring(dotIndex + 1) : "";
    }
    
    /**
     * Gets the timestamp when this event was created (milliseconds since epoch).
     */
    long getTimestamp();
}

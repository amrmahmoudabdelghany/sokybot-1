package org.sokybot.gameevents.store;

import java.time.Instant;
import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Interface for storing and retrieving game events.
 * Provides a journal of state changes for debugging and replay.
 */
public interface IEventStore {

    /**
     * Store an event.
     * 
     * @param event the game event to store
     */
    void store(IGameEvent event);

    /**
     * Get recent events for a specific machine.
     * 
     * @param machineId the machine ID (fullName)
     * @param limit     maximum number of events to return
     * @return list of events, newest first
     */
    List<IGameEvent> getRecentEvents(String machineId, int limit);

    /**
     * Get events within a time range for a machine.
     * 
     * @param machineId the machine ID
     * @param start     start time (inclusive)
     * @param end       end time (exclusive)
     * @return list of events ordered by time
     */
    List<IGameEvent> getEvents(String machineId, Instant start, Instant end);

    /**
     * Clear the event store for a machine.
     * 
     * @param machineId the machine ID
     */
    void clear(String machineId);

    /**
     * Get the total number of stored events for a machine.
     */
    int getEventCount(String machineId);

    /**
     * Get unique machine IDs that have events stored.
     */
    List<String> getMachineIds();
}

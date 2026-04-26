package org.sokybot.swarm.api;

/**
 * Terminal success: logistics exchange finished and the farmer can resume.
 */
public final class LogisticsCompletedEvent extends SwarmEvent {

    public LogisticsCompletedEvent(String requesterMachineId, long timestampEpochMs, String requestId) {
        super(requesterMachineId, timestampEpochMs, requestId);
    }
}

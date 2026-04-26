package org.sokybot.swarm.api;

import java.util.Objects;

/**
 * Base type for JVM-wide swarm logistics signals carried on {@link ISwarmEventBus}.
 */
public abstract class SwarmEvent {

    private final String requesterMachineId;
    private final long timestampEpochMs;
    private final String requestId;

    protected SwarmEvent(String requesterMachineId, long timestampEpochMs, String requestId) {
        this.requesterMachineId = Objects.requireNonNull(requesterMachineId, "requesterMachineId").trim();
        this.timestampEpochMs = timestampEpochMs;
        this.requestId = Objects.requireNonNull(requestId, "requestId").trim();
    }

    public String getRequesterMachineId() {
        return requesterMachineId;
    }

    public long getTimestampEpochMs() {
        return timestampEpochMs;
    }

    public String getRequestId() {
        return requestId;
    }
}

package org.sokybot.swarm.api;

/**
 * Terminal failure for a logistics request (disconnect, cancel, timeout, navigation, etc.).
 */
public final class LogisticsAbortedEvent extends SwarmEvent {

    public enum Reason {
        FARMER_DISCONNECTED,
        MULE_DISCONNECTED,
        EXCHANGE_CANCELLED,
        TIMEOUT,
        NAVIGATION_FAILED
    }

    private final Reason reason;

    public LogisticsAbortedEvent(String requesterMachineId, long timestampEpochMs, String requestId, Reason reason) {
        super(requesterMachineId, timestampEpochMs, requestId);
        this.reason = reason != null ? reason : Reason.TIMEOUT;
    }

    public Reason getReason() {
        return reason;
    }
}

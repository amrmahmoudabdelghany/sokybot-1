package org.sokybot.swarm.api;

import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;

/**
 * Treasury Mesh (Epic #19): assign supplier and distressed bots to meet at a rendezvous for a trade.
 * {@link #getRequestId()} is the {@code tradeSessionId}.
 */
public final class SwarmTradeDispatchEvent extends SwarmEvent {

    private final String tradeSessionId;
    private final String distressId;
    private final WorldPoint rendezvous;
    private final String supplierMachineId;
    private final String distressedMachineId;
    private final int itemRefId;
    private final int committedAmount;
    private final long rendezvousDeadlineEpochMs;

    public SwarmTradeDispatchEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String tradeSessionId,
            String distressId,
            WorldPoint rendezvous,
            String supplierMachineId,
            String distressedMachineId,
            int itemRefId,
            int committedAmount,
            long rendezvousDeadlineEpochMs) {
        super(requesterMachineId, timestampEpochMs, tradeSessionId);
        this.tradeSessionId = getRequestId();
        if (this.tradeSessionId.isEmpty()) {
            throw new IllegalArgumentException("tradeSessionId must be non-blank");
        }
        this.distressId = Objects.requireNonNull(distressId, "distressId").trim();
        this.rendezvous = Objects.requireNonNull(rendezvous, "rendezvous");
        this.supplierMachineId = Objects.requireNonNull(supplierMachineId, "supplierMachineId").trim();
        this.distressedMachineId = Objects.requireNonNull(distressedMachineId, "distressedMachineId").trim();
        this.itemRefId = itemRefId;
        this.committedAmount = committedAmount;
        this.rendezvousDeadlineEpochMs = rendezvousDeadlineEpochMs;
        if (this.distressId.isEmpty() || this.supplierMachineId.isEmpty() || this.distressedMachineId.isEmpty()) {
            throw new IllegalArgumentException("distressId, supplierMachineId, and distressedMachineId must be non-blank");
        }
        if (this.committedAmount <= 0) {
            throw new IllegalArgumentException("committedAmount must be positive");
        }
    }

    public String getTradeSessionId() {
        return tradeSessionId;
    }

    /** Assignee for this dispatch notification ({@link #getRequesterMachineId()}). */
    public String getAssigneeMachineId() {
        return getRequesterMachineId();
    }

    public String getDistressId() {
        return distressId;
    }

    public WorldPoint getRendezvous() {
        return rendezvous;
    }

    public String getSupplierMachineId() {
        return supplierMachineId;
    }

    public String getDistressedMachineId() {
        return distressedMachineId;
    }

    public int getItemRefId() {
        return itemRefId;
    }

    public int getCommittedAmount() {
        return committedAmount;
    }

    public long getRendezvousDeadlineEpochMs() {
        return rendezvousDeadlineEpochMs;
    }
}

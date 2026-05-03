package org.sokybot.swarm.api.router;

import java.util.Objects;

import org.sokybot.swarm.api.SwarmEvent;

/**
 * Epic #24 Silk Road Router: instruct a bot to initiate a player trade and move an item stack.
 */
public final class SwarmTradeCommandEvent extends SwarmEvent {

    private final String fromMachineId;
    private final String toMachineId;
    private final String uniqueItemId;
    private final int itemRefId;
    private final int quantity;
    private final String tradeSessionId;

    public SwarmTradeCommandEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String fromMachineId,
            String toMachineId,
            String uniqueItemId,
            int itemRefId,
            int quantity,
            String tradeSessionId) {
        super(requesterMachineId, timestampEpochMs, requestId);
        Objects.requireNonNull(fromMachineId, "fromMachineId");
        Objects.requireNonNull(toMachineId, "toMachineId");
        Objects.requireNonNull(uniqueItemId, "uniqueItemId");
        Objects.requireNonNull(tradeSessionId, "tradeSessionId");
        if (fromMachineId.trim().isEmpty()) {
            throw new IllegalArgumentException("fromMachineId must not be blank");
        }
        if (toMachineId.trim().isEmpty()) {
            throw new IllegalArgumentException("toMachineId must not be blank");
        }
        if (uniqueItemId.trim().isEmpty()) {
            throw new IllegalArgumentException("uniqueItemId must not be blank");
        }
        if (tradeSessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("tradeSessionId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.fromMachineId = fromMachineId.trim();
        this.toMachineId = toMachineId.trim();
        this.uniqueItemId = uniqueItemId.trim();
        this.itemRefId = itemRefId;
        this.quantity = quantity;
        this.tradeSessionId = tradeSessionId.trim();
    }

    public String getFromMachineId() {
        return fromMachineId;
    }

    public String getToMachineId() {
        return toMachineId;
    }

    public String getUniqueItemId() {
        return uniqueItemId;
    }

    public int getItemRefId() {
        return itemRefId;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getTradeSessionId() {
        return tradeSessionId;
    }
}

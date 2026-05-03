package org.sokybot.swarm.api;

import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;

/**
 * Treasury Mesh (Epic #19): broadcast that a bot needs consumables; surplus bots may reply with offers.
 */
public final class SwarmResourceDistressEvent extends SwarmEvent {

    private final String distressId;
    private final int itemRefId;
    private final int deficitAmount;
    private final WorldPoint distressedPosition;
    private final long offerDeadlineEpochMs;

    public SwarmResourceDistressEvent(
            String distressedMachineId,
            long timestampEpochMs,
            String distressId,
            int itemRefId,
            int deficitAmount,
            WorldPoint distressedPosition,
            long offerDeadlineEpochMs) {
        super(distressedMachineId, timestampEpochMs, distressId);
        this.distressId = getRequestId();
        if (this.distressId.isEmpty()) {
            throw new IllegalArgumentException("distressId must be non-blank");
        }
        this.itemRefId = itemRefId;
        this.deficitAmount = deficitAmount;
        this.distressedPosition = Objects.requireNonNull(distressedPosition, "distressedPosition");
        this.offerDeadlineEpochMs = offerDeadlineEpochMs;
        if (this.deficitAmount <= 0) {
            throw new IllegalArgumentException("deficitAmount must be positive");
        }
    }

    public String getDistressId() {
        return distressId;
    }

    /** Machine id of the bot broadcasting distress ({@link #getRequesterMachineId()}). */
    public String getDistressedMachineId() {
        return getRequesterMachineId();
    }

    public int getItemRefId() {
        return itemRefId;
    }

    public int getDeficitAmount() {
        return deficitAmount;
    }

    public WorldPoint getDistressedPosition() {
        return distressedPosition;
    }

    public long getOfferDeadlineEpochMs() {
        return offerDeadlineEpochMs;
    }
}

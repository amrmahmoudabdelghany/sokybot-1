package org.sokybot.swarm.api;

import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;

/**
 * Treasury Mesh (Epic #19): surplus bot reply to a {@link SwarmResourceDistressEvent}.
 */
public final class SwarmResourceOfferEvent extends SwarmEvent {

    private final String distressId;
    private final int itemRefId;
    private final int offeredAmount;
    private final WorldPoint offererPosition;

    public SwarmResourceOfferEvent(
            String offererMachineId,
            long timestampEpochMs,
            String distressId,
            int itemRefId,
            int offeredAmount,
            WorldPoint offererPosition) {
        super(offererMachineId, timestampEpochMs, distressId);
        this.distressId = getRequestId();
        if (this.distressId.isEmpty()) {
            throw new IllegalArgumentException("distressId must be non-blank");
        }
        if (getRequesterMachineId().isEmpty()) {
            throw new IllegalArgumentException("offererMachineId must be non-blank");
        }
        this.itemRefId = itemRefId;
        this.offeredAmount = offeredAmount;
        this.offererPosition = Objects.requireNonNull(offererPosition, "offererPosition");
        if (this.offeredAmount <= 0) {
            throw new IllegalArgumentException("offeredAmount must be positive");
        }
    }

    public String getDistressId() {
        return distressId;
    }

    public int getItemRefId() {
        return itemRefId;
    }

    public int getOfferedAmount() {
        return offeredAmount;
    }

    public WorldPoint getOffererPosition() {
        return offererPosition;
    }

    public String getOffererMachineId() {
        return getRequesterMachineId();
    }
}

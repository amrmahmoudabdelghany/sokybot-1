package org.sokybot.trade.coordination.api;

import java.util.Objects;

/**
 * Farmer-side query when looking for a mule (v1: open trust; only farmer id required).
 */
public final class FarmerProfile {

    private final String farmerMachineId;

    public FarmerProfile(String farmerMachineId) {
        String id = Objects.requireNonNull(farmerMachineId, "farmerMachineId").trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("farmerMachineId cannot be empty");
        }
        this.farmerMachineId = id;
    }

    public String getFarmerMachineId() {
        return farmerMachineId;
    }
}

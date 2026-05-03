package org.sokybot.swarm.api.router;

import java.util.Objects;

import org.sokybot.swarm.api.SwarmEvent;

/**
 * Epic #24 Silk Road Router: fired when a bot's inventory utilization crosses a critical threshold.
 */
public final class SwarmInventoryCriticalEvent extends SwarmEvent {

    private final String swarmGroupId;
    private final String reporterMachineId;
    private final double utilizationRatio;

    public SwarmInventoryCriticalEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String swarmGroupId,
            String reporterMachineId,
            double utilizationRatio) {
        super(requesterMachineId, timestampEpochMs, requestId);
        Objects.requireNonNull(swarmGroupId, "swarmGroupId");
        Objects.requireNonNull(reporterMachineId, "reporterMachineId");
        if (swarmGroupId.trim().isEmpty()) {
            throw new IllegalArgumentException("swarmGroupId must not be blank");
        }
        if (reporterMachineId.trim().isEmpty()) {
            throw new IllegalArgumentException("reporterMachineId must not be blank");
        }
        if (!Double.isFinite(utilizationRatio)) {
            throw new IllegalArgumentException("utilizationRatio must be finite");
        }
        if (utilizationRatio < 0.0d || utilizationRatio > 1.0d) {
            throw new IllegalArgumentException("utilizationRatio must be between 0.0 and 1.0");
        }
        this.swarmGroupId = swarmGroupId.trim();
        this.reporterMachineId = reporterMachineId.trim();
        this.utilizationRatio = utilizationRatio;
    }

    public String getSwarmGroupId() {
        return swarmGroupId;
    }

    public String getReporterMachineId() {
        return reporterMachineId;
    }

    public double getUtilizationRatio() {
        return utilizationRatio;
    }
}

package org.sokybot.behaviors.logistics;

import org.sokybot.swarm.api.LogisticsRequestEvent;
import org.sokybot.trade.coordination.api.SwarmRole;

import lombok.Data;

@Data
public class LogisticsSettings {

    private SwarmRole role = SwarmRole.NONE;
    private boolean muleSummonEnabled = true;
    private double muleSummonThreshold = 0.95;
    private LogisticsRequestEvent.Priority defaultPriority = LogisticsRequestEvent.Priority.NORMAL;
    private long requestTtlMs = 90_000L;
    private boolean fallbackToTownIfNoMule = true;

    /** Mule-side: minimum free inventory slots before claiming jobs. */
    private int minMuleFreeSlots = 4;

    /** Mule-side: squared horizontal distance (world units) considered "arrived" at farmer. */
    private int interactionRadiusSq = 625;

    /** Cross-region teleport routing toggle. */
    private boolean crossRegionEnabled = true;

    /** Route guardrail: hard cap on total teleport route gold cost. */
    private long maxRouteCostGold = 250_000L;

    /** Route guardrail: max number of teleport hops in route. */
    private int maxHopCount = 4;

    /** Route and hop interaction radius in world units. */
    private float interactionRadius = 25.0f;

    /** Mule-side: max time from claim to exchange completion before abort. */
    private long jobTimeoutMs = 120_000L;
}

package org.sokybot.behaviors.party.internal.blackhole;

import java.util.List;
import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;

/**
 * Shared Time-To-Target (TTT) planning for synchronized lure convergence (Epic #14).
 */
public final class BlackHoleTttPlanner {

    private BlackHoleTttPlanner() {
    }

    /**
     * @param anchor      kill-zone center
     * @param lurers      current world positions (one per assigned lurer; may be synthetic)
     * @param speed       assumed travel speed in world units per second ({@code > 0})
     * @param killRadius  inner radius of the kill zone (world units); travel demand is clamped to the outer approach ring
     * @return max ETA in milliseconds across lurers; {@code 0} when {@code lurers} is empty
     */
    public static long computeTttMs(WorldPoint anchor, List<WorldPoint> lurers, float speed, float killRadius) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(lurers, "lurers");
        if (lurers.isEmpty() || speed <= 0f) {
            return 0L;
        }
        float kr = Math.max(0f, killRadius);
        long maxEta = 0L;
        for (WorldPoint lurer : lurers) {
            if (lurer == null) {
                continue;
            }
            double dist = Math.max(0.0, anchor.distanceTo(lurer) - kr);
            long eta = (long) Math.ceil((dist / speed) * 1000.0);
            if (eta > maxEta) {
                maxEta = eta;
            }
        }
        return maxEta;
    }
}

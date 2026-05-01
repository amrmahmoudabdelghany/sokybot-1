package org.sokybot.behaviors.party.internal.blackhole;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public final class BlackHoleAnchorSettings {
    private boolean blackHoleAnchorEnabled;
    private float killRadiusWorld = 25.0f;
    private int nukeSkillRefId = -1;
    private List<String> assignedLurerMachineIds = new ArrayList<>();
    private float fanRadiusWorld = 40.0f;
    private long syncFudgeMs = 250L;

    /** Wall-clock dwell in PULLING before this anchor publishes CONVERGING (testing / staging). */
    private long pullingPhaseDurationMs = 5000L;

    /** Assumed walk speed for {@link BlackHoleTttPlanner} (world units per second). */
    private float tttAssumedWalkSpeedWorldPerSec = 12.0f;
}

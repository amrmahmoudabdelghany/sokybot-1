package org.sokybot.behaviors.combat.internal.settings;

import lombok.Data;

/**
 * Epic #20 Sentinel Protocol: PvP defense mesh settings (scope {@code sentinel}).
 */
@Data
public class SentinelSettings {

    private boolean sentinelEnabled = false;

    private long threatMemoryMs = 300_000L;

    private boolean retaliateOnly = true;
}

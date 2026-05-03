package org.sokybot.behaviors.party.internal.settings.shapeshifter;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Epic #22 Shapeshifter Protocol: dynamic role flex policy (scope {@code shapeshifter}).
 */
@Data
public class ShapeshifterSettings {

    private boolean shapeshifterEnabled = false;

    private List<FallbackRule> rules = new ArrayList<>();

    private long settleDelayMs = 5000L;

    private long cooldownMs = 30000L;
}

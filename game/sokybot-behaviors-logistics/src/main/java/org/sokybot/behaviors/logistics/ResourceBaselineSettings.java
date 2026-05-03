package org.sokybot.behaviors.logistics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Treasury Mesh (Epic #19): desired consumable levels per bot for surplus/distress decisions.
 */
@Data
public class ResourceBaselineSettings {

    private boolean enabled = false;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Map<Integer, Integer> itemTargets = new LinkedHashMap<>();

    private int distressThresholdPercent = 20;

    private long minOfferIntervalMs = 10_000L;

    /**
     * Defensive copy; mutating the returned map has no effect on stored settings.
     */
    public Map<Integer, Integer> getItemTargets() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(itemTargets));
    }

    /**
     * Stores a copy of the given map; {@code null} is treated as empty.
     */
    public void setItemTargets(Map<Integer, Integer> itemTargets) {
        this.itemTargets = itemTargets == null ? new LinkedHashMap<>() : new LinkedHashMap<>(itemTargets);
    }
}

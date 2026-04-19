package org.sokybot.town.api;

import java.util.Map;

/**
 * A declarative snapshot of why an {@link IntentKind} is active (telemetry, UX, arbitration logs).
 */
public interface IIntent {

    IntentKind getKind();

    /** Short human-readable reason ("inventory full", "durability 12%", …). */
    String getReason();

    /** Epoch millis when this intent was declared (wall clock). */
    long getDeclaredAtEpochMillis();

    /** Owning machine full name (group + machine). */
    String getMachineFullName();

    /**
     * Arbitrary diagnostic metadata; treat as immutable from the consumer side.
     */
    Map<String, String> getMetadata();
}

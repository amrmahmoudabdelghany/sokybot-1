package org.sokybot.swarm.api.sentinel;

import java.util.Set;

/**
 * Epic #20 Sentinel Protocol: JVM-wide hostile player names with expiry (implementation in behaviors-swarm).
 */
public interface IThreatLedgerCache {

    void upsertHostile(String characterName, long expiryEpochMs);

    boolean isHostile(String characterName);

    Set<String> getActiveHostiles(long currentTimeMs);
}

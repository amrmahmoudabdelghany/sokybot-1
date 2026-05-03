package org.sokybot.swarm.api.symphony;

import java.util.Optional;

/**
 * Epic #21: JVM-wide last-known symphony intent/effect signals for cross-bot coordination.
 */
public interface ISymphonyComboSignalCache {

    void recordIntent(SwarmCombatIntentEvent event);

    void recordEffect(SwarmCombatEffectEvent event);

    Optional<SwarmCombatIntentEvent> peekLatestIntent(String comboId, int targetRefId);

    Optional<SwarmCombatEffectEvent> peekLatestEffect(String comboId, int targetRefId);
}

package org.sokybot.gameevents.events.core;

import java.util.Map;
import java.util.List;

import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Factory for creating game-scoped translator instances.
 * Creates all translator instances with injected IGameDataLookup.
 * Allows different translator implementations per game if needed.
 */
public interface ITranslatorFactory {
    
    /**
     * Create translator chains for a specific game.
     * Each chain is ordered by descending provider priority.
     *
     * @param lookup The game-specific data lookup service, or {@code null} if not
     *            registered yet.
     * @return Map of opcode to ordered translator chain
     */
    Map<Integer, List<IPacketTranslator>> createTranslators(IGameDataLookup lookup,
            org.sokybot.network.IPacketPublisher publisher);

    /**
     * Legacy single-translator view.
     *
     * @deprecated Prefer {@link #createTranslators(IGameDataLookup, org.sokybot.network.IPacketPublisher)}
     *             and execute full chains.
     */
    @Deprecated
    default Map<Integer, IPacketTranslator> createTranslatorsSingle(IGameDataLookup lookup,
            org.sokybot.network.IPacketPublisher publisher) {
        Map<Integer, List<IPacketTranslator>> chains = createTranslators(lookup, publisher);
        Map<Integer, IPacketTranslator> flattened = new java.util.HashMap<>();
        for (Map.Entry<Integer, List<IPacketTranslator>> entry : chains.entrySet()) {
            List<IPacketTranslator> chain = entry.getValue();
            if (chain != null && !chain.isEmpty()) {
                flattened.put(entry.getKey(), chain.get(0));
            }
        }
        return flattened;
    }
}

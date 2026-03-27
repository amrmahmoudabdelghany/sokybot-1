package org.sokybot.gameevents.events.core;

import java.util.Map;

import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Factory for creating game-scoped translator instances.
 * Creates all translator instances with injected IGameDataLookup.
 * Allows different translator implementations per game if needed.
 */
public interface ITranslatorFactory {
    
    /**
     * Create all translator instances for a specific game.
     * Each translator gets the game's lookup service injected.
     * 
     * @param lookup The game-specific data lookup service, or {@code null} if not registered yet — script translators
     *            (e.g. gateway 0xA101) may still be created.
     * @return Map of opcode to translator instance
     */
    Map<Integer, IPacketTranslator> createTranslators(IGameDataLookup lookup, org.sokybot.network.IPacketPublisher publisher);
}

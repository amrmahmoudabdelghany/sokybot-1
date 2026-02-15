package org.sokybot.gameevents;

import java.util.Collections;
import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Base class for translators that need game data lookups.
 * Subclasses have access to IGameDataLookup via protected field.
 * Instances are created per-game (shared across all bots) for memory
 * optimization.
 * 
 * <p>
 * Translators must be thread-safe since they're shared across bots.
 * Per-bot state (like ChunkedPacketManager) is accessed via
 * ChunkedPacketManagerRegistry.
 */
public abstract class AbstractTranslator implements IPacketTranslator {

    /**
     * Game-specific data lookup service.
     * Provides access to NPCEntity, ItemEntity, SkillEntity, etc.
     * Shared per game - all bots in the same game use the same lookup.
     */
    protected final IGameDataLookup lookup;

    /**
     * Constructor with game data lookup.
     * Called by ITranslatorProvider when creating per-game instances.
     * 
     * @param lookup The game-specific lookup service
     */
    public AbstractTranslator(IGameDataLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * Default constructor for translators that don't need lookups.
     */
    public AbstractTranslator() {
        this(null);
    }

    /**
     * Translate packet to events.
     * Delegates to translateInternal() without chunk manager parameter.
     * Chunk manager is accessed via registry when needed.
     * 
     * @param machineFullName The machine identifier
     * @param packet          The packet to translate
     * @param chunkManager    Not used (kept for backward compatibility)
     * @return List of events
     */
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet,
            ChunkedPacketManager chunkManager) {
        // Use registry to access chunk manager - no need to pass as parameter
        return translateInternal(machineFullName, packet);
    }

    /**
     * Backward compatibility method for tests.
     * Delegates to translate(String, ImmutablePacket, ChunkedPacketManager) with
     * null chunkManager.
     * 
     * @deprecated Use translate(String, ImmutablePacket, ChunkedPacketManager)
     *             instead
     */
    @Deprecated
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        return translate(machineFullName, packet, null);
    }

    /**
     * Internal translation method.
     * Subclasses implement this method. Chunk manager can be accessed via
     * getChunkManager().
     * 
     * @param machineFullName The machine identifier
     * @param packet          The packet to translate
     * @return List of events
     */
    protected abstract List<IGameEvent> translateInternal(String machineFullName,
            ImmutablePacket packet);

    /**
     * Get the chunk manager for the current machine from the registry.
     * Returns null if not registered or not needed.
     * 
     * <p>
     * Translators that need chunk manager (e.g., CharacterDataBeginTranslator,
     * CharacterDataChunkTranslator) should call this method.
     * 
     * @param machineFullName The machine identifier
     * @return The chunk manager, or null if not available
     */
    protected ChunkedPacketManager getChunkManager(String machineFullName) {
        return ChunkedPacketManagerRegistry.getInstance().get(machineFullName);
    }

    /**
     * Helper: Wrap a single event in a list.
     */
    protected List<IGameEvent> singleEvent(IGameEvent event) {
        return event != null ? List.of(event) : Collections.emptyList();
    }

    /**
     * Helper: Return empty list (for chunk translators that don't emit events).
     */
    protected List<IGameEvent> noEvents() {
        return Collections.emptyList();
    }
}

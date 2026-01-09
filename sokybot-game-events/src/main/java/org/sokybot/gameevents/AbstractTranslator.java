package org.sokybot.gameevents;

import java.util.Collections;
import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Base class for translators that need game data lookups.
 * Subclasses have access to IGameDataLookup via protected field.
 * Instances are created per-game by ITranslatorFactory.
 */
public abstract class AbstractTranslator implements IPacketTranslator {
    
    /**
     * Game-specific data lookup service.
     * Provides access to NPCEntity, ItemEntity, SkillEntity, etc.
     */
    protected final IGameDataLookup lookup;
    
    /**
     * Chunked packet manager for handling multi-packet transactions.
     * Can be set after construction via setChunkManager().
     */
    protected ChunkedPacketManager chunkManager;
    
    /**
     * Constructor with game data lookup and chunk manager injection.
     * Called by TranslatorFactory when creating per-game instances.
     * 
     * @param lookup The game-specific lookup service
     * @param chunkManager The chunked packet manager for multi-packet transactions
     */
    public AbstractTranslator(IGameDataLookup lookup, ChunkedPacketManager chunkManager) {
        this.lookup = lookup;
        this.chunkManager = chunkManager;
    }
    
    /**
     * Constructor with lookup only (for translators that don't need chunk management).
     */
    public AbstractTranslator(IGameDataLookup lookup) {
        this(lookup, null);
    }
    
    /**
     * Default constructor for translators that don't need lookups or chunks.
     */
    public AbstractTranslator() {
        this(null, null);
    }
    
    /**
     * Set the chunk manager for this translator.
     * Used by TranslatorFactory to inject shared ChunkedPacketManager.
     * 
     * @param chunkManager The chunked packet manager to use
     */
    public void setChunkManager(ChunkedPacketManager chunkManager) {
        this.chunkManager = chunkManager;
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



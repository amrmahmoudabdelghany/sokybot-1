package org.sokybot.gameevents;

import org.sokybot.api.events.IPacketTranslator;
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
     * Constructor with game data lookup injection.
     * Called by TranslatorFactory when creating per-game instances.
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
        this.lookup = null;
    }
}

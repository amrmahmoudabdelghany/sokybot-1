package org.sokybot.persistence.service;

import javax.persistence.EntityManagerFactory;
import java.util.Map;

/**
 * Manages EntityManagerFactory instances per game path.
 * Each game gets its own database file and persistence context.
 * This allows for complete isolation between different game installations.
 */
public interface IPersistenceContextManager {
    
    /**
     * Get or create EntityManagerFactory for a specific game.
     * The EntityManagerFactory is created on first access and cached.
     * 
     * @param gamePath The game installation path (unique identifier)
     * @return EntityManagerFactory for this game's database
     */
    EntityManagerFactory getEntityManagerFactory(String gamePath);
    
    /**
     * Remove and close EntityManagerFactory for a game.
     * Releases all resources associated with this game's database.
     * 
     * @param gamePath The game installation path
     */
    void closeEntityManagerFactory(String gamePath);
    
    /**
     * Get all active game contexts.
     * 
     * @return Map of game paths to their EntityManagerFactory instances
     */
    Map<String, EntityManagerFactory> getActiveContexts();
    
    /**
     * Check if a game context exists.
     * 
     * @param gamePath The game installation path
     * @return true if context exists, false otherwise
     */
    boolean hasContext(String gamePath);
}
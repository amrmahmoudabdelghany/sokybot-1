package org.sokybot.persistence.service;

import java.util.Optional;

import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.entities.ObjectNavMesh;
import org.sokybot.persistence.entities.SectorRef;
import org.sokybot.persistence.entities.SkillEntity;

/**
 * Lightweight lookup interface for game data access.
 * Provides per-game entity lookups for translators.
 * Each game instance has its own IGameDataLookup.
 */
public interface IGameDataLookup {
    
    /**
     * Find NPC/Monster entity by refId
     */
    Optional<NPCEntity> findNPC(int refId);
    
    /**
     * Find Item entity by refId
     */
    Optional<ItemEntity> findItem(int refId);
    
    /**
     * Find Skill entity by refId
     */
    Optional<SkillEntity> findSkill(int refId);
    
    /**
     * Find mastery name by ID
     */
    Optional<String> findMasteryName(int masteryId);
    
    /**
     * Get the game path this lookup is associated with
     */
    String getGamePath();
    
    /**
     * Find sector reference by sectorYX.
     * Used for navigation mesh data.
     */
    Optional<SectorRef> findSector(short sectorYX);
    
    /**
     * Find object nav mesh by object ID.
     * Used for navigation mesh data.
     */
    Optional<ObjectNavMesh> findObjectNavMesh(int objectId);
}

package org.sokybot.game.asset;

import java.awt.Image;
import java.util.Optional;

/**
 * Provides access to binary media assets that are read from game files.
 * These assets (images) are cached in-memory rather than persisted to database.
 * 
 * <p>Use this interface for:
 * <ul>
 *   <li>Minimap tiles from Media.pk2</li>
 *   <li>Character/NPC icons</li>
 *   <li>Skill icons</li>
 *   <li>Item icons</li>
 * </ul>
 */
public interface IMediaAssetProvider {
    
    /**
     * Find sector minimap tile by coordinates.
     * Minimap tiles are DDJ files from Media.pk2/minimap/
     * 
     * @param x sector X coordinate
     * @param y sector Y coordinate
     * @return the minimap Image, or empty if not found
     */
    Optional<Image> findSectorMinimap(short x, short y);
    
    /**
     * Find character/NPC icon by character ID.
     * 
     * @param charId the character refId
     * @return the icon Image, or empty if not found
     */
    Optional<Image> findCharacterIcon(int charId);
    
    /**
     * Find skill icon by skill ID.
     * 
     * @param skillId the skill refId
     * @return the icon Image, or empty if not found
     */
    Optional<Image> findSkillIcon(int skillId);
    
    /**
     * Find item icon by item ID.
     * 
     * @param itemId the item refId
     * @return the icon Image, or empty if not found
     */
    Optional<Image> findItemIcon(int itemId);
}

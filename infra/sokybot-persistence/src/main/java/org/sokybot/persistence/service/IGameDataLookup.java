package org.sokybot.persistence.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sokybot.persistence.entities.DivisionInfo;
import org.sokybot.persistence.entities.GameInfo;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.entities.ObjectNavMesh;
import org.sokybot.persistence.entities.PortalEntity;
import org.sokybot.persistence.entities.SectorRef;
import org.sokybot.persistence.entities.ShopEntity;
import org.sokybot.persistence.entities.SilkroadType;
import org.sokybot.persistence.entities.SkillEntity;
import org.sokybot.persistence.entities.TeleportDestinationEntity;
import org.sokybot.persistence.entities.TeleportEntity;

/**
 * Unified lookup interface for game data access.
 * Provides per-game entity lookups.
 * Each game instance has its own IGameDataLookup.
 * 
 * <p>
 * This interface consolidates methods for:
 * <ul>
 * <li>Sector/navmesh lookups</li>
 * <li>Entity lookups (NPC, Item, Skill, etc.)</li>
 * <li>Game connection info</li>
 * <li>Monster search</li>
 * </ul>
 * 
 * <p>
 * <b>Note:</b> Path finding is handled by {@code IRuteFinder} from
 * sokybot-game-navigation.
 * <p>
 * <b>Note:</b> Media assets (icons, minimaps) are handled by
 * {@code IMediaAssetProvider} from sokybot-game-asset.
 */
public interface IGameDataLookup {

    // ============================================================
    // Entity Lookups (from IMediaPk2)
    // ============================================================

    /**
     * Find NPC/Monster entity by refId.
     */
    Optional<NPCEntity> findNPC(int refId);

    /**
     * Find Item entity by refId.
     */
    Optional<ItemEntity> findItem(int refId);

    /**
     * Find Skill entity by refId.
     */
    Optional<SkillEntity> findSkill(int refId);

    /**
     * Find Shop entity by NPC refId.
     */
    Optional<ShopEntity> findShop(int npcRefId);

    /**
     * Find Teleport entity by refId.
     */
    Optional<TeleportEntity> findTeleport(int refId);

    default java.util.stream.Stream<TeleportEntity> findAllTeleports() {
        return java.util.stream.Stream.empty();
    }

    default Optional<TeleportDestinationEntity> findTeleportDestination(int destinationRefId) {
        return Optional.empty();
    }

    /**
     * Find Portal entity by refId.
     */
    Optional<PortalEntity> findPortal(int refId);

    /**
     * Find mastery name by ID.
     */
    Optional<String> findMasteryName(int masteryId);

    /**
     * Get experience required for a level.
     */
    Optional<Long> getLvlEXP(int lvl);

    // ============================================================
    // Sector/Navigation Lookups (from IDataPk2)
    // ============================================================

    /**
     * Find sector reference by sectorYX.
     * Used for navigation mesh data.
     */
    Optional<SectorRef> findSector(short sectorYX);

    /**
     * Find sector reference by X/Y coordinates.
     */
    default Optional<SectorRef> findSector(byte sectorX, byte sectorY) {
        return findSector((short) (((sectorY & 0xFF) << 8) | (sectorX & 0xFF)));
    }

    /**
     * Find all sectors in the game.
     */
    List<SectorRef> findAllSectors();

    /**
     * Find object nav mesh by object ID.
     * Used for navigation mesh data.
     */
    Optional<ObjectNavMesh> findObjectNavMesh(int objectId);

    // ============================================================
    // Monster Search (from ISroMaterialDAO)
    // ============================================================

    /**
     * Find all monsters matching a name filter.
     */
    List<NPCEntity> findAllMonsterLike(String filter);

    // ============================================================
    // Game Info (from ISroDAO)
    // ============================================================

    /**
     * Get the game path this lookup is associated with.
     */
    String getGamePath();

    /**
     * Get the game server port.
     */
    int getPort();

    /**
     * Get the game version.
     */
    int getVersion();

    /**
     * Get the game's Silkroad type (locale, etc.).
     */
    Optional<SilkroadType> findType();

    /**
     * Get the game's division info.
     */
    Optional<DivisionInfo> findDivisionInfo();

    /**
     * Get division hosts map.
     */
    Map<String, List<String>> getDivHosts();

    /**
     * Get a random host from the division.
     */
    Optional<String> getRndHost();

    /**
     * Get the locale byte.
     */
    Optional<Byte> getLocal();

    /**
     * Get the game language.
     */
    Optional<String> getLanguage();

    /**
     * Get the game country.
     */
    Optional<String> getCountry();
}

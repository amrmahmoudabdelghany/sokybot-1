package org.sokybot.gameevents.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.sokybot.persistence.entities.DivisionInfo;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.entities.ObjectNavMesh;
import org.sokybot.persistence.entities.PortalEntity;
import org.sokybot.persistence.entities.SectorRef;
import org.sokybot.persistence.entities.ShopEntity;
import org.sokybot.persistence.entities.SilkroadType;
import org.sokybot.persistence.entities.SkillEntity;
import org.sokybot.persistence.entities.TeleportEntity;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Mock implementation of IGameDataLookup for testing.
 * Allows registering mock entities that will be returned by lookup methods.
 * Thread-safe for concurrent test execution.
 */
public class MockGameDataLookup implements IGameDataLookup {
    
    private final Map<Integer, NPCEntity> npcs = new ConcurrentHashMap<>();
    private final Map<Integer, ItemEntity> items = new ConcurrentHashMap<>();
    private final Map<Integer, SkillEntity> skills = new ConcurrentHashMap<>();
    private final Map<Integer, ShopEntity> shops = new ConcurrentHashMap<>();
    private final Map<Integer, TeleportEntity> teleports = new ConcurrentHashMap<>();
    private final Map<Integer, PortalEntity> portals = new ConcurrentHashMap<>();
    private final Map<Integer, String> masteryNames = new ConcurrentHashMap<>();
    private final Map<Integer, Long> levelExp = new ConcurrentHashMap<>();
    private final Map<Short, SectorRef> sectors = new ConcurrentHashMap<>();
    
    private String gamePath = "test/path";
    private int port = 15779;
    private int gameVersion = 1;
    
    /**
     * Register a mock NPC entity.
     */
    public MockGameDataLookup addNPC(int refId, NPCEntity npc) {
        npcs.put(refId, npc);
        return this;
    }
    
    /**
     * Register a mock NPC with minimal data.
     */
    public MockGameDataLookup addNPC(int refId, String name) {
        NPCEntity npc = new NPCEntity();
        npc.setRefId(refId);
        npc.setName(name);
        npcs.put(refId, npc);
        return this;
    }
    
    /**
     * Register a mock Item entity.
     */
    public MockGameDataLookup addItem(int refId, ItemEntity item) {
        items.put(refId, item);
        return this;
    }
    
    /**
     * Register a mock Item with minimal data.
     */
    public MockGameDataLookup addItem(int refId, String name) {
        ItemEntity item = new ItemEntity();
        item.setRefId(refId);
        item.setName(name);
        items.put(refId, item);
        return this;
    }
    
    /**
     * Register a mock Skill entity.
     */
    public MockGameDataLookup addSkill(int refId, SkillEntity skill) {
        skills.put(refId, skill);
        return this;
    }
    
    /**
     * Register a mock Skill with minimal data.
     */
    public MockGameDataLookup addSkill(int refId, String name) {
        SkillEntity skill = new SkillEntity();
        skill.setRefId(refId);
        skill.setName(name);
        skills.put(refId, skill);
        return this;
    }
    
    /**
     * Register a mock Shop entity.
     */
    public MockGameDataLookup addShop(int npcRefId, ShopEntity shop) {
        shops.put(npcRefId, shop);
        return this;
    }
    
    /**
     * Register a mock Teleport entity.
     */
    public MockGameDataLookup addTeleport(int refId, TeleportEntity teleport) {
        teleports.put(refId, teleport);
        return this;
    }
    
    /**
     * Register a mock Portal entity.
     */
    public MockGameDataLookup addPortal(int refId, PortalEntity portal) {
        portals.put(refId, portal);
        return this;
    }
    
    /**
     * Register a mastery name.
     */
    public MockGameDataLookup addMasteryName(int masteryId, String name) {
        masteryNames.put(masteryId, name);
        return this;
    }
    
    /**
     * Register experience required for a level.
     */
    public MockGameDataLookup addLevelExp(int level, long exp) {
        levelExp.put(level, exp);
        return this;
    }
    
    /**
     * Register a sector reference.
     */
    public MockGameDataLookup addSector(short sectorYX, SectorRef sector) {
        sectors.put(sectorYX, sector);
        return this;
    }
    
    /**
     * Set the game path.
     */
    public MockGameDataLookup setGamePath(String path) {
        this.gamePath = path;
        return this;
    }
    
    /**
     * Set the game port.
     */
    public MockGameDataLookup setPort(int port) {
        this.port = port;
        return this;
    }
    
    /**
     * Set the game version.
     */
    public MockGameDataLookup setGameVersion(int version) {
        this.gameVersion = version;
        return this;
    }
    
    // IGameDataLookup implementation
    
    @Override
    public Optional<NPCEntity> findNPC(int refId) {
        return Optional.ofNullable(npcs.get(refId));
    }
    
    @Override
    public Optional<ItemEntity> findItem(int refId) {
        return Optional.ofNullable(items.get(refId));
    }
    
    @Override
    public Optional<SkillEntity> findSkill(int refId) {
        return Optional.ofNullable(skills.get(refId));
    }
    
    @Override
    public Optional<ShopEntity> findShop(int npcRefId) {
        return Optional.ofNullable(shops.get(npcRefId));
    }
    
    @Override
    public Optional<TeleportEntity> findTeleport(int refId) {
        return Optional.ofNullable(teleports.get(refId));
    }
    
    @Override
    public Optional<PortalEntity> findPortal(int refId) {
        return Optional.ofNullable(portals.get(refId));
    }
    
    @Override
    public Optional<String> findMasteryName(int masteryId) {
        return Optional.ofNullable(masteryNames.get(masteryId));
    }
    
    @Override
    public Optional<Long> getLvlEXP(int lvl) {
        return Optional.ofNullable(levelExp.get(lvl));
    }
    
    @Override
    public Optional<SectorRef> findSector(short sectorYX) {
        return Optional.ofNullable(sectors.get(sectorYX));
    }
    
    @Override
    public List<SectorRef> findAllSectors() {
        return List.copyOf(sectors.values());
    }
    
    @Override
    public Optional<ObjectNavMesh> findObjectNavMesh(int objectId) {
        // Not commonly used in tests - return empty by default
        return Optional.empty();
    }
    
    @Override
    public List<NPCEntity> findAllMonsterLike(String filter) {
        // Filter NPCs by name containing filter (case-insensitive)
        List<NPCEntity> results = new ArrayList<>();
        String filterLower = filter != null ? filter.toLowerCase() : "";
        for (NPCEntity npc : npcs.values()) {
            if (npc.getName() != null && npc.getName().toLowerCase().contains(filterLower)) {
                results.add(npc);
            }
        }
        return results;
    }
    
    @Override
    public String getGamePath() {
        return gamePath;
    }
    
    @Override
    public int getPort() {
        return port;
    }
    
    @Override
    public int getVersion() {
        return gameVersion;
    }
    
    @Override
    public Optional<SilkroadType> findType() {
        // Not commonly used in tests - return empty by default
        return Optional.empty();
    }
    
    @Override
    public Optional<DivisionInfo> findDivisionInfo() {
        // Not commonly used in tests - return empty by default
        return Optional.empty();
    }
    
    @Override
    public Map<String, List<String>> getDivHosts() {
        // Not commonly used in tests - return empty map
        return Collections.emptyMap();
    }
    
    @Override
    public Optional<String> getRndHost() {
        // Not commonly used in tests - return empty
        return Optional.empty();
    }
    
    @Override
    public Optional<Byte> getLocal() {
        // Not commonly used in tests - return empty
        return Optional.empty();
    }
    
    @Override
    public Optional<String> getLanguage() {
        // Not commonly used in tests - return empty
        return Optional.empty();
    }
    
    @Override
    public Optional<String> getCountry() {
        // Not commonly used in tests - return empty
        return Optional.empty();
    }
}

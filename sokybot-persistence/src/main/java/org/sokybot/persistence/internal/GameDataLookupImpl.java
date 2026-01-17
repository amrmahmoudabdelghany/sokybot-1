package org.sokybot.persistence.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;

import org.sokybot.persistence.entities.DivisionInfo;
import org.sokybot.persistence.entities.GameInfo;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.entities.LvlEXP;
import org.sokybot.persistence.entities.MasteryData;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.entities.ObjectNavMesh;
import org.sokybot.persistence.entities.PortalEntity;
import org.sokybot.persistence.entities.SectorRef;
import org.sokybot.persistence.entities.ShopEntity;
import org.sokybot.persistence.entities.SilkroadType;
import org.sokybot.persistence.entities.SkillEntity;
import org.sokybot.persistence.entities.TeleportEntity;
import org.sokybot.persistence.internal.extraction.CachedExtractionDecorator;
import org.sokybot.persistence.internal.extraction.IPk2ExtractionHandler;
import org.sokybot.persistence.internal.extraction.ItemExtractionHandler;
import org.sokybot.persistence.internal.extraction.NPCExtractionHandler;
import org.sokybot.persistence.internal.extraction.RetryableExtractionDecorator;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.persistence.service.PersistenceException;
import org.sokybot.pk2extractor.dto.character.NPCData;
import org.sokybot.pk2extractor.dto.item.ItemData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-game lookup implementation backed by JPA/Database.
 * Each game instance has its own GameDataLookupImpl backed by its own database.
 */
public class GameDataLookupImpl implements IGameDataLookup {
    
    private static final Logger logger = LoggerFactory.getLogger(GameDataLookupImpl.class);

    private final String gamePath;
    private final EntityManagerFactory emf;
    
    // Per-game repository instances
    private final NPCEntityRepositoryImpl npcRepository;
    private final ItemEntityRepositoryImpl itemRepository;
    private final SkillEntityRepositoryImpl skillRepository;
    private final ShopEntityRepositoryImpl shopRepository;
    private final TeleportEntityRepositoryImpl teleportRepository;
    private final PortalEntityRepositoryImpl portalRepository;
    private final SectorRefRepositoryImpl sectorRepository;
    private final ObjectNavMeshRepositoryImpl objectNavMeshRepository;
    private final LvlEXPRepositoryImpl lvlEXPRepository;
    private final MasteryDataRepositoryImpl masteryRepository;
    private final GameInfoRepositoryImpl gameInfoRepository;
    
    // PK2 extraction handlers with decorators
    private final IPk2ExtractionHandler<NPCData, NPCEntity> npcExtractionHandler;
    private final IPk2ExtractionHandler<ItemData, ItemEntity> itemExtractionHandler;
    
    public GameDataLookupImpl(String gamePath, EntityManagerFactory emf) {
        this.gamePath = gamePath;
        this.emf = emf;
        
        // Create per-game repository instances with this game's EMF
        this.npcRepository = new NPCEntityRepositoryImpl(emf);
        this.itemRepository = new ItemEntityRepositoryImpl(emf);
        this.skillRepository = new SkillEntityRepositoryImpl(emf);
        this.shopRepository = new ShopEntityRepositoryImpl(emf);
        this.teleportRepository = new TeleportEntityRepositoryImpl(emf);
        this.portalRepository = new PortalEntityRepositoryImpl(emf);
        this.sectorRepository = new SectorRefRepositoryImpl(emf);
        this.objectNavMeshRepository = new ObjectNavMeshRepositoryImpl(emf);
        this.lvlEXPRepository = new LvlEXPRepositoryImpl(emf);
        this.masteryRepository = new MasteryDataRepositoryImpl(emf);
        this.gameInfoRepository = new GameInfoRepositoryImpl(emf);
        
        // Create extraction handlers with decorators
        // Base handlers
        IPk2ExtractionHandler<NPCData, NPCEntity> baseNpcHandler = new NPCExtractionHandler(emf);
        IPk2ExtractionHandler<ItemData, ItemEntity> baseItemHandler = new ItemExtractionHandler(emf);
        
        // Add retry decorator (3 retries with 1000ms delay)
        IPk2ExtractionHandler<NPCData, NPCEntity> retryableNpcHandler = 
            new RetryableExtractionDecorator<>(baseNpcHandler, 3, 1000);
        IPk2ExtractionHandler<ItemData, ItemEntity> retryableItemHandler = 
            new RetryableExtractionDecorator<>(baseItemHandler, 3, 1000);
        
        // Add caching decorator (skip if already extracted)
        this.npcExtractionHandler = new CachedExtractionDecorator<>(
            retryableNpcHandler,
            () -> npcRepository.count() > 0
        );
        
        this.itemExtractionHandler = new CachedExtractionDecorator<>(
            retryableItemHandler,
            () -> itemRepository.count() > 0
        );
    }
    
    @Override
    public String getGamePath() {
        return gamePath;
    }
    
    @Override
    public Optional<NPCEntity> findNPC(int refId) {
        Optional<NPCEntity> entity = npcRepository.findById(refId);
        if (entity.isPresent()) {
            return entity;
        }
        
        // Lazy load NPCs if not found
        synchronized(this) {
            // Double-check pattern
            entity = npcRepository.findById(refId);
            if (entity.isPresent()) {
                return entity;
            }
            
            importNPCs();
            return npcRepository.findById(refId);
        }
    }
    
    @Override
    public Optional<ItemEntity> findItem(int refId) {
        Optional<ItemEntity> entity = itemRepository.findById(refId);
        if (entity.isPresent()) {
            return entity;
        }
        
        // Lazy load Items if not found
        synchronized(this) {
            // Double-check pattern
            entity = itemRepository.findById(refId);
            if (entity.isPresent()) {
                return entity;
            }
            
            importItems();
            return itemRepository.findById(refId);
        }
    }
    
    @Override
    public Optional<SkillEntity> findSkill(int refId) {
        return skillRepository.findById(refId);
    }

    @Override
    public Optional<ShopEntity> findShop(int npcRefId) {
        return shopRepository.findById(npcRefId);
    }

    @Override
    public Optional<TeleportEntity> findTeleport(int refId) {
        return teleportRepository.findById(refId);
    }

    @Override
    public Optional<PortalEntity> findPortal(int refId) {
        return portalRepository.findById(refId);
    }
    
    @Override
    public Optional<String> findMasteryName(int masteryId) {
        return masteryRepository.findById(masteryId).map(MasteryData::getName);
    }

    @Override
    public Optional<Long> getLvlEXP(int lvl) {
        return lvlEXPRepository.findById(lvl).map(LvlEXP::getExp);
    }
    
    @Override
    public Optional<SectorRef> findSector(short sectorYX) {
        return sectorRepository.findById(sectorYX);
    }
    
    @Override
    public List<SectorRef> findAllSectors() {
        return sectorRepository.findAll();
    }

    @Override
    public Optional<ObjectNavMesh> findObjectNavMesh(int objectId) {
        return objectNavMeshRepository.findById(objectId);
    }

    @Override
    public List<NPCEntity> findAllMonsterLike(String filter) {
        return npcRepository.findAllMonsterLike(filter);
    }

    @Override
    public Optional<SilkroadType> findType() {
        return findGameInfo().map(GameInfo::getSilkroadType);
    }

    @Override
    public Optional<DivisionInfo> findDivisionInfo() {
        return findGameInfo().map(GameInfo::getDivisionInfo);
    }

    @Override
    public Map<String, List<String>> getDivHosts() {
        return findDivisionInfo()
                .map(info -> info.getDivisions().stream()
                        .collect(Collectors.toMap(
                                div -> div.getName(), 
                                div -> div.getHosts()
                        )))
                .orElse(Collections.emptyMap());
    }

    @Override
    public Optional<String> getRndHost() {
        return findDivisionInfo()
                .flatMap(info -> info.getDivisions().stream().findFirst())
                .map(div -> div.getRandomHost());
    }

    @Override
    public Optional<Byte> getLocal() {
        return findDivisionInfo().map(info -> info.local);
    }

    @Override
    public Optional<String> getLanguage() {
        return findType().map(SilkroadType::getLanguage);
    }

    @Override
    public Optional<String> getCountry() {
        return findType().map(SilkroadType::getCountry);
    }

    @Override
    public int getPort() {
        return findGameInfo().map(GameInfo::getPort).orElse(-1);
    }

    @Override
    public int getVersion() {
        return findGameInfo().map(GameInfo::getVersion).orElse(-1);
    }

    // Helper methods
    
    private Optional<GameInfo> findGameInfo() {
        // GameInfo uses gamePath as ID
        return gameInfoRepository.findById(this.gamePath); 
    }
    
    /**
     * Import NPCs from PK2 file using the extraction handler with decorators.
     */
    private void importNPCs() {
        try {
            npcExtractionHandler.extractAndPersist(gamePath, "Media.pk2");
        } catch (PersistenceException e) {
            logger.error("Failed to import NPCs for game: " + gamePath, e);
            throw new RuntimeException("Failed to import NPCs", e);
        }
    }

    /**
     * Import Items from PK2 file using the extraction handler with decorators.
     */
    private void importItems() {
        try {
            itemExtractionHandler.extractAndPersist(gamePath, "Media.pk2");
        } catch (PersistenceException e) {
            logger.error("Failed to import Items for game: " + gamePath, e);
            throw new RuntimeException("Failed to import Items", e);
        }
    }
}
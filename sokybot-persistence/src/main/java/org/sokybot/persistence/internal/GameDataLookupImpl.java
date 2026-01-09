package org.sokybot.persistence.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;

import org.sokybot.persistence.entities.DivisionInfo;
import org.sokybot.persistence.entities.GameInfo;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.entities.LvlEXP;
import org.sokybot.persistence.entities.MasteryData;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.entities.NPCType;
import org.sokybot.persistence.entities.ObjectNavMesh;
import org.sokybot.persistence.entities.PortalEntity;
import org.sokybot.persistence.entities.SectorRef;
import org.sokybot.persistence.entities.ShopEntity;
import org.sokybot.persistence.entities.SilkroadType;
import org.sokybot.persistence.entities.SkillEntity;
import org.sokybot.persistence.entities.TeleportEntity;
import org.sokybot.persistence.entities.Gender;
import org.sokybot.persistence.entities.Race;
import org.sokybot.persistence.entities.ItemType;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.dto.character.NPCData;
import org.sokybot.pk2extractor.dto.item.ItemData;
import org.sokybot.pk2extractor.mediapk2.character.NPCDataExtractor;
import org.sokybot.pk2extractor.mediapk2.item.ItemDataExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-game lookup implementation backed by JPA/Database.
 * Each game instance has its own GameDataLookupImpl backed by its own database.
 */
public class GameDataLookupImpl implements IGameDataLookup {
    
    private final Logger logger = LoggerFactory.getLogger(GameDataLookupImpl.class);

    private final String gamePath;
    private final EntityManagerFactory emf;
    
    public GameDataLookupImpl(String gamePath, EntityManagerFactory emf) {
        this.gamePath = gamePath;
        this.emf = emf;
    }
    
    @Override
    public String getGamePath() {
        return gamePath;
    }
    
    @Override
    public Optional<NPCEntity> findNPC(int refId) {
        Optional<NPCEntity> entity = find(NPCEntity.class, refId);
        if (entity.isPresent()) return entity;
        
        synchronized(this) {
             entity = find(NPCEntity.class, refId);
             if(entity.isPresent()) return entity;
             
             importNPCs();
             return find(NPCEntity.class, refId);
        }
    }
    
    @Override
    public Optional<ItemEntity> findItem(int refId) {
        Optional<ItemEntity> entity = find(ItemEntity.class, refId);
        if (entity.isPresent()) return entity;
        
        synchronized(this) {
            entity = find(ItemEntity.class, refId);
            if (entity.isPresent()) return entity;
            
            importItems();
            return find(ItemEntity.class, refId);
        }
    }
    
    @Override
    public Optional<SkillEntity> findSkill(int refId) {
        return find(SkillEntity.class, refId);
        // TODO: Implement lazy loading for skills
    }

    @Override
    public Optional<ShopEntity> findShop(int npcRefId) {
        return find(ShopEntity.class, npcRefId);
    }

    @Override
    public Optional<TeleportEntity> findTeleport(int refId) {
        return find(TeleportEntity.class, refId);
    }

    @Override
    public Optional<PortalEntity> findPortal(int refId) {
        return find(PortalEntity.class, refId);
    }
    
    @Override
    public Optional<String> findMasteryName(int masteryId) {
        return find(MasteryData.class, masteryId).map(MasteryData::getName);
    }

    @Override
    public Optional<Long> getLvlEXP(int lvl) {
        return find(LvlEXP.class, lvl).map(LvlEXP::getExp);
    }
    
    @Override
    public Optional<SectorRef> findSector(short sectorYX) {
        return find(SectorRef.class, sectorYX);
    }
    
    @Override
    public List<SectorRef> findAllSectors() {
        return execute(em -> {
            TypedQuery<SectorRef> query = em.createQuery("SELECT s FROM SectorRef s", SectorRef.class);
            return query.getResultList();
        });
    }

    @Override
    public Optional<ObjectNavMesh> findObjectNavMesh(int objectId) {
        return find(ObjectNavMesh.class, objectId);
    }

    @Override
    public List<NPCEntity> findAllMonsterLike(String filter) {
         return execute(em -> {
            TypedQuery<NPCEntity> query = em.createQuery("SELECT n FROM NPCEntity n WHERE n.name LIKE :filter", NPCEntity.class);
            query.setParameter("filter", "%" + filter + "%");
            return query.getResultList();
        });
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
    
    private <T> Optional<T> find(Class<T> entityClass, Object primaryKey) {
        EntityManager em = emf.createEntityManager();
        try {
            return Optional.ofNullable(em.find(entityClass, primaryKey));
        } finally {
            em.close();
        }
    }
    
    private Optional<GameInfo> findGameInfo() {
        // GameInfo uses gamePath as ID
        return find(GameInfo.class, this.gamePath); 
    }
    
    private <T> T execute(EntityManagerFunction<T> action) {
        EntityManager em = emf.createEntityManager();
        try {
            return action.apply(em);
        } finally {
            em.close();
        }
    }
    
    private void importNPCs() {
        logger.info("Importing NPC Data from PK2...");
        try (IPk2Driver driver = IPk2Driver.open(new File(gamePath, "Media.pk2").getAbsolutePath())) {
            NPCDataExtractor extractor = new NPCDataExtractor();
            EntityManager em = emf.createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                extractor.extract(driver, new org.sokybot.pk2extractor.ExtractionListener<NPCData>() {
                    @Override
                    public void onExtracted(NPCData dto) {
                        try {
                            NPCEntity entity = NPCEntity.builder()
                                    .refId(dto.getRefId())
                                    .longId(dto.getLongId())
                                    .name(dto.getName())
                                    .level(dto.getLevel())
                                    .HP(dto.getHP())
                                    .Type(dto.getType() != null ? NPCType.of(dto.getType().getValue()) : null)
                                    .iconPath(dto.getIconPath())
                                    .build();
                            em.merge(entity);
                        } catch (Exception e) {
                            logger.error("Failed to merge NPC entity: " + dto.getLongId(), e);
                        }
                    }
                    @Override public void onComplete(int totalCount) {}
                    @Override public void onError(Exception error) {
                        logger.error("Extraction error", error);
                    }
                }, null);
                tx.commit();
            } catch(Exception e) {
                if(tx.isActive()) tx.rollback();
                throw e;
            } finally {
                em.close();
            }
        } catch (IOException e) {
            logger.error("Failed to import NPC data", e);
        }
    }

    private void importItems() {
        logger.info("Importing Item Data from PK2...");
        try (IPk2Driver driver = IPk2Driver.open(new File(gamePath, "Media.pk2").getAbsolutePath())) {
            ItemDataExtractor extractor = new ItemDataExtractor();
            EntityManager em = emf.createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                extractor.extract(driver, new org.sokybot.pk2extractor.ExtractionListener<ItemData>() {
                    @Override
                    public void onExtracted(ItemData dto) {
                        try {
                             ItemEntity entity = ItemEntity.builder()
                                    .refId(dto.getRefId())
                                    .longId(dto.getLongId())
                                    .name(dto.getName())
                                    .isMallItem(dto.isMallItem())
                                    .iconPath(dto.getIconPath())
                                    .level(dto.getLevel())
                                    .degree(dto.getDegree())
                                    .maxStacks(dto.getMaxStacks())
                                    .isSortable(dto.isSortable())
                                    .isSOX(dto.isSOX())
                                    .itemType(dto.getItemType() != null ? ItemType.parseType(dto.getItemType().getValue(), dto.getLongId()) : null)
                                    .race(dto.getRace() != null ? Race.parseType((byte)dto.getRace().getValue()) : null)
                                    .gender(dto.getGender() != null ? Gender.parseType((byte)dto.getGender().getValue()) : null)
                                    .build();
                            em.merge(entity);
                        } catch(Exception e) {
                             logger.error("Failed to merge Item entity: " + dto.getLongId(), e);
                        }
                    }
                    @Override public void onComplete(int totalCount) {}
                    @Override public void onError(Exception error) {
                         logger.error("Extraction error", error);
                    }
                }, null);
                tx.commit();
            } catch(Exception e) {
                if(tx.isActive()) tx.rollback();
                throw e;
            } finally {
                em.close();
            }
        } catch (IOException e) {
             logger.error("Failed to import Item data", e);
        }
    }

    @FunctionalInterface
    private interface EntityManagerFunction<T> {
        T apply(EntityManager em);
    }
}

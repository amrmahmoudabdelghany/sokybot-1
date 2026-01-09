package org.sokybot.persistence.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

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
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Per-game lookup implementation backed by JPA/Database.
 * Each game instance has its own GameDataLookupImpl backed by its own database.
 */
public class GameDataLookupImpl implements IGameDataLookup {
    
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
        return find(NPCEntity.class, refId);
    }
    
    @Override
    public Optional<ItemEntity> findItem(int refId) {
        return find(ItemEntity.class, refId);
    }
    
    @Override
    public Optional<SkillEntity> findSkill(int refId) {
        return find(SkillEntity.class, refId);
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
    
    @FunctionalInterface
    private interface EntityManagerFunction<T> {
        T apply(EntityManager em);
    }
}

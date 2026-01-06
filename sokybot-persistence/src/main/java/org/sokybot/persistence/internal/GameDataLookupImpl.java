package org.sokybot.persistence.internal;

import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.entities.MasteryData;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.entities.SkillEntity;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Per-game lookup implementation.
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
        EntityManager em = emf.createEntityManager();
        try {
            NPCEntity result = em.find(NPCEntity.class, refId);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<ItemEntity> findItem(int refId) {
        EntityManager em = emf.createEntityManager();
        try {
            ItemEntity result = em.find(ItemEntity.class, refId);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<SkillEntity> findSkill(int refId) {
        EntityManager em = emf.createEntityManager();
        try {
            SkillEntity result = em.find(SkillEntity.class, refId);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<String> findMasteryName(int masteryId) {
        EntityManager em = emf.createEntityManager();
        try {
            MasteryData result = em.find(MasteryData.class, masteryId);
            return Optional.ofNullable(result).map(MasteryData::getName);
        } finally {
            em.close();
        }
    }
}

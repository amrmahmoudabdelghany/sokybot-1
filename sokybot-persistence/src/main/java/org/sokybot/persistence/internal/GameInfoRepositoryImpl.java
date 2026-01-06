package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.GameInfo;
import org.sokybot.persistence.service.GameInfoRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Component(service = GameInfoRepository.class)
public class GameInfoRepositoryImpl implements GameInfoRepository {
    
    private EntityManagerFactory emf;
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    @Override
    public GameInfo save(GameInfo entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            GameInfo result = em.merge(entity);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save GameInfo", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<GameInfo> saveAll(List<GameInfo> entities) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (GameInfo entity : entities) {
                em.merge(entity);
            }
            em.getTransaction().commit();
            return entities;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save GameInfo list", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<GameInfo> findById(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            GameInfo result = em.find(GameInfo.class, id);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<GameInfo> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT n FROM GameInfo n", GameInfo.class)
                     .getResultList();
        } finally {
            em.close();
        }
    }
    
    @Override
    public void deleteById(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            GameInfo entity = em.find(GameInfo.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete GameInfo", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public void delete(GameInfo entity) {
        if (entity != null && entity.getGamePath() != null) {
            deleteById(entity.getGamePath());
        }
    }
    
    @Override
    public void deleteAll() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM GameInfo").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete all GameInfo", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean existsById(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(n) FROM GameInfo n WHERE n.gamePath = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }
    
    @Override
    public long count() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(n) FROM GameInfo n", Long.class)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }

}

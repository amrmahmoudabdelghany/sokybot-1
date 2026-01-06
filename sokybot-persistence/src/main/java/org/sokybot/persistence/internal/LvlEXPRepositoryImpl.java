package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.LvlEXP;
import org.sokybot.persistence.service.LvlEXPRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Component(service = LvlEXPRepository.class)
public class LvlEXPRepositoryImpl implements LvlEXPRepository {
    
    private EntityManagerFactory emf;
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    @Override
    public LvlEXP save(LvlEXP entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            LvlEXP result = em.merge(entity);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save LvlEXP", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<LvlEXP> saveAll(List<LvlEXP> entities) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (LvlEXP entity : entities) {
                em.merge(entity);
            }
            em.getTransaction().commit();
            return entities;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save LvlEXP list", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<LvlEXP> findById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            LvlEXP result = em.find(LvlEXP.class, id);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<LvlEXP> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT n FROM LvlEXP n", LvlEXP.class)
                     .getResultList();
        } finally {
            em.close();
        }
    }
    
    @Override
    public void deleteById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            LvlEXP entity = em.find(LvlEXP.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete LvlEXP", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public void delete(LvlEXP entity) {
        if (entity != null && entity.getLevel() != 0) {
            deleteById(entity.getLevel());
        }
    }
    
    @Override
    public void deleteAll() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM LvlEXP").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete all LvlEXP", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean existsById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(n) FROM LvlEXP n WHERE n.level = :id", Long.class)
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
            return em.createQuery("SELECT COUNT(n) FROM LvlEXP n", Long.class)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }

}

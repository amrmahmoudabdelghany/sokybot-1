package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.Settings;
import org.sokybot.persistence.service.SettingsRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;

@Component(service = SettingsRepository.class)
public class SettingsRepositoryImpl implements SettingsRepository {
    
    private EntityManagerFactory emf;
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    @Override
    public Settings save(Settings entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Settings result = em.merge(entity);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save Settings", e);
        } finally {
            em.close();
        }
    }

    @Override
    public Settings saveAndFlush(Settings entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Settings result = em.merge(entity);
            em.flush();
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to saveAndFlush Settings", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<Settings> saveAll(List<Settings> entities) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (Settings entity : entities) {
                em.merge(entity);
            }
            em.getTransaction().commit();
            return entities;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save Settings list", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<Settings> findById(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            Settings result = em.find(Settings.class, id);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<Settings> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT s FROM Settings s", Settings.class)
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
            Settings entity = em.find(Settings.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete Settings", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public void delete(Settings entity) {
        if (entity != null && entity.getId() != null) {
            deleteById(entity.getId());
        }
    }
    
    @Override
    public void deleteAll() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Settings").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete all Settings", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean existsById(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(s) FROM Settings s WHERE s.id = :id", Long.class)
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
            return em.createQuery("SELECT COUNT(s) FROM Settings s", Long.class)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }
}

package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.service.ItemEntityRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Component(service = ItemEntityRepository.class)
public class ItemEntityRepositoryImpl implements ItemEntityRepository {
    
    private EntityManagerFactory emf;
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    @Override
    public ItemEntity save(ItemEntity entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            ItemEntity result = em.merge(entity);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save ItemEntity", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<ItemEntity> saveAll(List<ItemEntity> entities) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (ItemEntity entity : entities) {
                em.merge(entity);
            }
            em.getTransaction().commit();
            return entities;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save ItemEntity list", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<ItemEntity> findById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            ItemEntity result = em.find(ItemEntity.class, id);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<ItemEntity> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT n FROM ItemEntity n", ItemEntity.class)
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
            ItemEntity entity = em.find(ItemEntity.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete ItemEntity", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public void delete(ItemEntity entity) {
        if (entity != null && entity.getRefId() != 0) {
            deleteById(entity.getRefId());
        }
    }
    
    @Override
    public void deleteAll() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM ItemEntity").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete all ItemEntity", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean existsById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(n) FROM ItemEntity n WHERE n.refId = :id", Long.class)
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
            return em.createQuery("SELECT COUNT(n) FROM ItemEntity n", Long.class)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<ItemEntity> findItemEntityByLongId(String longId) {
        EntityManager em = emf.createEntityManager();
        try {
            List<ItemEntity> results = em.createQuery(
                "SELECT i FROM ItemEntity i WHERE i.longId = :longId", ItemEntity.class)
                .setParameter("longId", longId)
                .getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }
}

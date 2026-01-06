package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.ShopEntity;
import org.sokybot.persistence.service.ShopEntityRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Component(service = ShopEntityRepository.class)
public class ShopEntityRepositoryImpl implements ShopEntityRepository {
    
    private EntityManagerFactory emf;
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    @Override
    public ShopEntity save(ShopEntity entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            ShopEntity result = em.merge(entity);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save ShopEntity", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<ShopEntity> saveAll(List<ShopEntity> entities) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (ShopEntity entity : entities) {
                em.merge(entity);
            }
            em.getTransaction().commit();
            return entities;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save ShopEntity list", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<ShopEntity> findById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            ShopEntity result = em.find(ShopEntity.class, id);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<ShopEntity> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT n FROM ShopEntity n", ShopEntity.class)
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
            ShopEntity entity = em.find(ShopEntity.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete ShopEntity", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public void delete(ShopEntity entity) {
        if (entity != null) {
            deleteById(entity.getShopId());
        }
    }
    
    @Override
    public void deleteAll() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM ShopEntity").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete all ShopEntity", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean existsById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(n) FROM ShopEntity n WHERE n.shopId = :id", Long.class)
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
            return em.createQuery("SELECT COUNT(n) FROM ShopEntity n", Long.class)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }

}

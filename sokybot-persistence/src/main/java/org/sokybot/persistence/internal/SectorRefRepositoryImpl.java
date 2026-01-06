package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.SectorRef;
import org.sokybot.persistence.service.SectorRefRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Component(service = SectorRefRepository.class)
public class SectorRefRepositoryImpl implements SectorRefRepository {
    
    private EntityManagerFactory emf;
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    @Override
    public SectorRef save(SectorRef entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            SectorRef result = em.merge(entity);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save SectorRef", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<SectorRef> saveAll(List<SectorRef> entities) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (SectorRef entity : entities) {
                em.merge(entity);
            }
            em.getTransaction().commit();
            return entities;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save SectorRef list", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<SectorRef> findById(Short id) {
        EntityManager em = emf.createEntityManager();
        try {
            SectorRef result = em.find(SectorRef.class, id);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<SectorRef> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT n FROM SectorRef n", SectorRef.class)
                     .getResultList();
        } finally {
            em.close();
        }
    }
    
    @Override
    public void deleteById(Short id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            SectorRef entity = em.find(SectorRef.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete SectorRef", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public void delete(SectorRef entity) {
        if (entity != null && entity.getSectorYX() != 0) {
            deleteById(entity.getSectorYX());
        }
    }
    
    @Override
    public void deleteAll() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM SectorRef").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete all SectorRef", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean existsById(Short id) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(n) FROM SectorRef n WHERE n.sectorYX = :id", Long.class)
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
            return em.createQuery("SELECT COUNT(n) FROM SectorRef n", Long.class)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }

}

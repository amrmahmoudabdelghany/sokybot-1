package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.ObjectNavMesh;
import org.sokybot.persistence.service.ObjectNavMeshRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Component(service = ObjectNavMeshRepository.class)
public class ObjectNavMeshRepositoryImpl implements ObjectNavMeshRepository {
    
    private EntityManagerFactory emf;
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    @Override
    public ObjectNavMesh save(ObjectNavMesh entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            ObjectNavMesh result = em.merge(entity);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save ObjectNavMesh", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<ObjectNavMesh> saveAll(List<ObjectNavMesh> entities) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (ObjectNavMesh entity : entities) {
                em.merge(entity);
            }
            em.getTransaction().commit();
            return entities;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save ObjectNavMesh list", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<ObjectNavMesh> findById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            ObjectNavMesh result = em.find(ObjectNavMesh.class, id);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<ObjectNavMesh> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT n FROM ObjectNavMesh n", ObjectNavMesh.class)
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
            ObjectNavMesh entity = em.find(ObjectNavMesh.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete ObjectNavMesh", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public void delete(ObjectNavMesh entity) {
        if (entity != null) {
            deleteById(entity.getId());
        }
    }
    
    @Override
    public void deleteAll() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM ObjectNavMesh").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete all ObjectNavMesh", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean existsById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(n) FROM ObjectNavMesh n WHERE n.id = :id", Long.class)
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
            return em.createQuery("SELECT COUNT(n) FROM ObjectNavMesh n", Long.class)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }

}

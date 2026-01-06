package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.service.NPCEntityRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Component(service = NPCEntityRepository.class)
public class NPCEntityRepositoryImpl implements NPCEntityRepository {
    
    private EntityManagerFactory emf;
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    @Override
    public NPCEntity save(NPCEntity entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            NPCEntity result = em.merge(entity);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save NPCEntity", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<NPCEntity> saveAll(List<NPCEntity> entities) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (NPCEntity entity : entities) {
                em.merge(entity);
            }
            em.getTransaction().commit();
            return entities;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save NPCEntity list", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<NPCEntity> findById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            NPCEntity result = em.find(NPCEntity.class, id);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<NPCEntity> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT n FROM NPCEntity n", NPCEntity.class)
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
            NPCEntity entity = em.find(NPCEntity.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete NPCEntity", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public void delete(NPCEntity entity) {
        if (entity != null && entity.getRefId() != 0) {
            deleteById(entity.getRefId());
        }
    }
    
    @Override
    public void deleteAll() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM NPCEntity").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete all NPCEntity", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean existsById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(n) FROM NPCEntity n WHERE n.refId = :id", Long.class)
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
            return em.createQuery("SELECT COUNT(n) FROM NPCEntity n", Long.class)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<NPCEntity> findAllMonsterLike(String filter) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<NPCEntity> query = em.createQuery(
                "SELECT e FROM NPCEntity e WHERE e.longId LIKE '%MOB_%' AND (e.level LIKE :filter OR e.name LIKE :filter)", 
                NPCEntity.class);
            query.setParameter("filter", "%" + filter + "%");
            return query.getResultList();
        } finally {
            em.close();
        }
    }
}

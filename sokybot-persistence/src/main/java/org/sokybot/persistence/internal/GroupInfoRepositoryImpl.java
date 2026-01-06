package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.domain.GroupInfo;
import org.sokybot.persistence.service.GroupInfoRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

/**
 * OSGi Declarative Services component implementing GroupInfo repository.
 */
@Component(service = GroupInfoRepository.class)
public class GroupInfoRepositoryImpl implements GroupInfoRepository {
    
    private EntityManagerFactory emf;
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    @Override
    public GroupInfo save(GroupInfo entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            GroupInfo result = em.merge(entity);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save GroupInfo", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<GroupInfo> findById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            GroupInfo result = em.find(GroupInfo.class, id);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<GroupInfo> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT g FROM GroupInfo g", GroupInfo.class)
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
            GroupInfo entity = em.find(GroupInfo.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete GroupInfo with id: " + id, e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public void delete(GroupInfo entity) {
        if (entity != null && entity.getId() != 0) {
            deleteById(entity.getId());
        }
    }
    
    @Override
    public boolean existsById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(g) FROM GroupInfo g WHERE g.id = :id", Long.class)
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
            return em.createQuery("SELECT COUNT(g) FROM GroupInfo g", Long.class)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<GroupInfo> findByName(String name) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<GroupInfo> query = em.createQuery(
                "SELECT g FROM GroupInfo g WHERE g.name = :name", GroupInfo.class);
            query.setParameter("name", name);
            
            List<GroupInfo> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<GroupInfo> findByGamePath(String gamePath) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<GroupInfo> query = em.createQuery(
                "SELECT g FROM GroupInfo g WHERE g.gamePath = :gamePath", GroupInfo.class);
            query.setParameter("gamePath", gamePath);
            
            List<GroupInfo> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }
}

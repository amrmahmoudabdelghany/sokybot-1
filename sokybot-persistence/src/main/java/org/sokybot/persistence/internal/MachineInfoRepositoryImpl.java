package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.app.domain.MachineInfo;
import org.sokybot.persistence.service.MachineInfoRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

/**
 * OSGi Declarative Services component implementing MachineInfo repository.
 */
@Component(service = MachineInfoRepository.class)
public class MachineInfoRepositoryImpl implements MachineInfoRepository {
    
    private EntityManagerFactory emf;
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    @Override
    public MachineInfo save(MachineInfo entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            MachineInfo result = em.merge(entity);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save MachineInfo", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<MachineInfo> findById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            MachineInfo result = em.find(MachineInfo.class, id);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<MachineInfo> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT m FROM MachineInfo m", MachineInfo.class)
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
            MachineInfo entity = em.find(MachineInfo.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete MachineInfo with id: " + id, e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public void delete(MachineInfo entity) {
        if (entity != null && entity.getId() != 0) {
            deleteById(entity.getId());
        }
    }
    
    @Override
    public boolean existsById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(m) FROM MachineInfo m WHERE m.id = :id", Long.class)
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
            return em.createQuery("SELECT COUNT(m) FROM MachineInfo m", Long.class)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<MachineInfo> findByGroupId(int groupId) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<MachineInfo> query = em.createQuery(
                "SELECT m FROM MachineInfo m WHERE m.group.id = :groupId", MachineInfo.class);
            query.setParameter("groupId", groupId);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<MachineInfo> findByMachineName(String machineName) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<MachineInfo> query = em.createQuery(
                "SELECT m FROM MachineInfo m WHERE m.machineName = :machineName", MachineInfo.class);
            query.setParameter("machineName", machineName);
            
            List<MachineInfo> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }
}
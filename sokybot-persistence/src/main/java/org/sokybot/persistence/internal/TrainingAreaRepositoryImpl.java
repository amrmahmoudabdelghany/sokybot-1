package org.sokybot.persistence.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.entities.TrainingArea;
import org.sokybot.persistence.service.TrainingAreaRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Component(service = TrainingAreaRepository.class)
public class TrainingAreaRepositoryImpl implements TrainingAreaRepository {
    
    private EntityManagerFactory emf;
    
    @Reference
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    @Override
    public TrainingArea save(TrainingArea entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            TrainingArea result = em.merge(entity);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save TrainingArea", e);
        } finally {
            em.close();
        }
    }

    @Override
    public TrainingArea saveAndFlush(TrainingArea entity) {
        // Since EMF transaction commit flushes, this is essentially same as save
        // But explicit flush can be done inside
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            TrainingArea result = em.merge(entity);
            em.flush();
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to saveAndFlush TrainingArea", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<TrainingArea> saveAll(List<TrainingArea> entities) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (TrainingArea entity : entities) {
                em.merge(entity);
            }
            em.getTransaction().commit();
            return entities;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save TrainingArea list", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public Optional<TrainingArea> findById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            TrainingArea result = em.find(TrainingArea.class, id);
            return Optional.ofNullable(result);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<TrainingArea> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT n FROM TrainingArea n", TrainingArea.class)
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
            TrainingArea entity = em.find(TrainingArea.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete TrainingArea", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public void delete(TrainingArea entity) {
        if (entity != null && entity.getId() != null) {
            deleteById(entity.getId());
        }
    }
    
    @Override
    public void deleteAll() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM TrainingArea").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete all TrainingArea", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean existsById(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(n) FROM TrainingArea n WHERE n.id = :id", Long.class)
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
            return em.createQuery("SELECT COUNT(n) FROM TrainingArea n", Long.class)
                     .getSingleResult();
        } finally {
            em.close();
        }
    }

}

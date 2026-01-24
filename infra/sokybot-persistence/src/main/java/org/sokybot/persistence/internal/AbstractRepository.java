package org.sokybot.persistence.internal;

import org.sokybot.persistence.service.IRepository;
import org.sokybot.persistence.service.PersistenceException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Generic base repository implementation with proper transaction management.
 * Provides common CRUD operations and separates read-only from transactional operations.
 * 
 * @param <T> Entity type
 * @param <ID> ID type
 */
public abstract class AbstractRepository<T, ID> implements IRepository<T, ID> {
    
    protected EntityManagerFactory emf;
    protected final Class<T> entityClass;
    
    protected AbstractRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }
    
    protected AbstractRepository(EntityManagerFactory emf, Class<T> entityClass) {
        this.emf = emf;
        this.entityClass = entityClass;
    }
    
    protected void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    protected EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            throw new IllegalStateException(
                "EntityManagerFactory not set for " + entityClass.getSimpleName());
        }
        return emf;
    }
    
    /**
     * Execute an operation within a transaction.
     * Transaction is automatically committed on success or rolled back on failure.
     */
    protected <R> R executeInTransaction(Function<EntityManager, R> operation) {
        EntityManager em = getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            R result = operation.apply(em);
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new PersistenceException(
                "Transaction failed for " + entityClass.getSimpleName(), e);
        } finally {
            em.close();
        }
    }
    
    /**
     * Execute a read-only operation without transaction overhead.
     */
    protected <R> R executeReadOnly(Function<EntityManager, R> operation) {
        EntityManager em = getEntityManagerFactory().createEntityManager();
        try {
            return operation.apply(em);
        } finally {
            em.close();
        }
    }
    
    @Override
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        return executeInTransaction(em -> em.merge(entity));
    }
    
    @Override
    public List<T> saveAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return entities;
        }
        return executeInTransaction(em -> {
            entities.forEach(entity -> {
                if (entity != null) {
                    em.merge(entity);
                }
            });
            return entities;
        });
    }
    
    @Override
    public Optional<T> findById(ID id) {
        if (id == null) {
            return Optional.empty();
        }
        return executeReadOnly(em -> 
            Optional.ofNullable(em.find(entityClass, id))
        );
    }
    
    @Override
    public List<T> findAll() {
        return executeReadOnly(em -> {
            String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e";
            TypedQuery<T> query = em.createQuery(jpql, entityClass);
            return query.getResultList();
        });
    }
    
    @Override
    public void deleteById(ID id) {
        if (id == null) {
            return;
        }
        executeInTransaction(em -> {
            T entity = em.find(entityClass, id);
            if (entity != null) {
                em.remove(entity);
            }
            return null;
        });
    }
    
    @Override
    public void delete(T entity) {
        if (entity == null) {
            return;
        }
        executeInTransaction(em -> {
            em.remove(em.contains(entity) ? entity : em.merge(entity));
            return null;
        });
    }
    
    @Override
    public void deleteAll() {
        executeInTransaction(em -> {
            String jpql = "DELETE FROM " + entityClass.getSimpleName();
            em.createQuery(jpql).executeUpdate();
            return null;
        });
    }
    
    @Override
    public boolean existsById(ID id) {
        if (id == null) {
            return false;
        }
        return executeReadOnly(em -> {
            String idFieldName = getIdFieldName();
            String jpql = "SELECT COUNT(e) FROM " + entityClass.getSimpleName() + 
                         " e WHERE e." + idFieldName + " = :id";
            Long count = em.createQuery(jpql, Long.class)
                .setParameter("id", id)
                .getSingleResult();
            return count > 0;
        });
    }
    
    @Override
    public long count() {
        return executeReadOnly(em -> {
            String jpql = "SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e";
            return em.createQuery(jpql, Long.class).getSingleResult();
        });
    }
    
    /**
     * Override this method to specify the ID field name for queries.
     * Default implementation tries common field names.
     */
    protected String getIdFieldName() {
        // Try common patterns
        String[] commonNames = {"id", "refId", "gamePath", "shopId", "sectorYX"};
        for (String fieldName : commonNames) {
            if (hasField(fieldName)) {
                return fieldName;
            }
        }
        throw new IllegalStateException(
            "Cannot determine ID field for " + entityClass.getName() + 
            ". Override getIdFieldName() to specify it.");
    }
    
    private boolean hasField(String fieldName) {
        try {
            Field field = entityClass.getDeclaredField(fieldName);
            return field != null;
        } catch (NoSuchFieldException e) {
            // Check parent classes
            Class<?> superClass = entityClass.getSuperclass();
            while (superClass != null && superClass != Object.class) {
                try {
                    superClass.getDeclaredField(fieldName);
                    return true;
                } catch (NoSuchFieldException ex) {
                    superClass = superClass.getSuperclass();
                }
            }
            return false;
        }
    }
}
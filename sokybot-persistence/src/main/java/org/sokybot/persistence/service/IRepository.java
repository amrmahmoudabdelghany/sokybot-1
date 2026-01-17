package org.sokybot.persistence.service;

import java.util.List;
import java.util.Optional;

/**
 * Base repository interface with common CRUD operations.
 * 
 * @param <T> Entity type
 * @param <ID> ID type
 */
public interface IRepository<T, ID> {
    
    T save(T entity);
    
    List<T> saveAll(List<T> entities);
    
    Optional<T> findById(ID id);
    
    List<T> findAll();
    
    void deleteById(ID id);
    
    void delete(T entity);
    
    void deleteAll();
    
    boolean existsById(ID id);
    
    long count();
}
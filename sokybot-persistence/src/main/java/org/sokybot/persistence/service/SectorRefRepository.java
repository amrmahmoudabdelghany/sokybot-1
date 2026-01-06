package org.sokybot.persistence.service;

import java.util.List;
import java.util.Optional;

import org.sokybot.persistence.entities.SectorRef;

public interface SectorRefRepository {

    SectorRef save(SectorRef entity);
    
    List<SectorRef> saveAll(List<SectorRef> entities);
    
    Optional<SectorRef> findById(Short id);
    
    List<SectorRef> findAll();
    
    void deleteById(Short id);
    
    void delete(SectorRef entity);
    
    void deleteAll();
    
    boolean existsById(Short id);
    
    long count();

}

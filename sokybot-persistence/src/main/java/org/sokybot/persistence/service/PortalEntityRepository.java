package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.PortalEntity;
import java.util.List;
import java.util.Optional;

public interface PortalEntityRepository {
    PortalEntity save(PortalEntity entity);
    List<PortalEntity> saveAll(List<PortalEntity> entities);
    Optional<PortalEntity> findById(Integer id);
    List<PortalEntity> findAll();
    void deleteById(Integer id);
    void delete(PortalEntity entity);
    void deleteAll();
    boolean existsById(Integer id);
    long count();
}

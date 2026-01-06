package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.TeleportEntity;
import java.util.List;
import java.util.Optional;

public interface TeleportEntityRepository {
    TeleportEntity save(TeleportEntity entity);
    List<TeleportEntity> saveAll(List<TeleportEntity> entities);
    Optional<TeleportEntity> findById(Integer id);
    List<TeleportEntity> findAll();
    void deleteById(Integer id);
    void delete(TeleportEntity entity);
    void deleteAll();
    boolean existsById(Integer id);
    long count();
}

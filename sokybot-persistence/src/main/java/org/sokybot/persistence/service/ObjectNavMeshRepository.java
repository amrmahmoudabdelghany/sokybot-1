package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.ObjectNavMesh;
import java.util.List;
import java.util.Optional;

public interface ObjectNavMeshRepository {
    ObjectNavMesh save(ObjectNavMesh entity);
    List<ObjectNavMesh> saveAll(List<ObjectNavMesh> entities);
    Optional<ObjectNavMesh> findById(Integer id);
    List<ObjectNavMesh> findAll();
    void deleteById(Integer id);
    void delete(ObjectNavMesh entity);
    void deleteAll();
    boolean existsById(Integer id);
    long count();
}

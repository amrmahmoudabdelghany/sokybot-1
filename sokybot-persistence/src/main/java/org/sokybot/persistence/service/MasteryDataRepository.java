package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.MasteryData;
import java.util.List;
import java.util.Optional;

public interface MasteryDataRepository {
    MasteryData save(MasteryData entity);
    List<MasteryData> saveAll(List<MasteryData> entities);
    Optional<MasteryData> findById(Integer id);
    List<MasteryData> findAll();
    void deleteById(Integer id);
    void delete(MasteryData entity);
    void deleteAll();
    boolean existsById(Integer id);
    long count();
}

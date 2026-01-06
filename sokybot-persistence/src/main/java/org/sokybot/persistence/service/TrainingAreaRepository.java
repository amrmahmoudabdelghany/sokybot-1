package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.TrainingArea;
import java.util.List;
import java.util.Optional;

public interface TrainingAreaRepository {
    TrainingArea save(TrainingArea entity);
    TrainingArea saveAndFlush(TrainingArea entity);
    List<TrainingArea> saveAll(List<TrainingArea> entities);
    Optional<TrainingArea> findById(Integer id);
    List<TrainingArea> findAll();
    void deleteById(Integer id);
    void delete(TrainingArea entity);
    void deleteAll();
    boolean existsById(Integer id);
    long count();
}

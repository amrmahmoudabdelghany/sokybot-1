package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.SkillEntity;
import java.util.List;
import java.util.Optional;

public interface SkillEntityRepository {
    SkillEntity save(SkillEntity entity);
    List<SkillEntity> saveAll(List<SkillEntity> entities);
    Optional<SkillEntity> findById(Integer id);
    List<SkillEntity> findAll();
    void deleteById(Integer id);
    void delete(SkillEntity entity);
    void deleteAll();
    boolean existsById(Integer id);
    long count();
}

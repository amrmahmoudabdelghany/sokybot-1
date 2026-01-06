package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.LvlEXP;
import java.util.List;
import java.util.Optional;

public interface LvlEXPRepository {
    LvlEXP save(LvlEXP entity);
    List<LvlEXP> saveAll(List<LvlEXP> entities);
    Optional<LvlEXP> findById(Integer level);
    List<LvlEXP> findAll();
    void deleteById(Integer level);
    void delete(LvlEXP entity);
    void deleteAll();
    boolean existsById(Integer level);
    long count();
}

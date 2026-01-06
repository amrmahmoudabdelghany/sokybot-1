package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.GameInfo;
import java.util.List;
import java.util.Optional;

public interface GameInfoRepository {
    GameInfo save(GameInfo entity);
    List<GameInfo> saveAll(List<GameInfo> entities);
    Optional<GameInfo> findById(String gamePath);
    List<GameInfo> findAll();
    void deleteById(String gamePath);
    void delete(GameInfo entity);
    void deleteAll();
    boolean existsById(String gamePath);
    long count();
}

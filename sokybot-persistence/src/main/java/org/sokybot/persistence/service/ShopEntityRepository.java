package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.ShopEntity;
import java.util.List;
import java.util.Optional;

public interface ShopEntityRepository {
    ShopEntity save(ShopEntity entity);
    List<ShopEntity> saveAll(List<ShopEntity> entities);
    Optional<ShopEntity> findById(Integer id);
    List<ShopEntity> findAll();
    void deleteById(Integer id);
    void delete(ShopEntity entity);
    void deleteAll();
    boolean existsById(Integer id);
    long count();
}

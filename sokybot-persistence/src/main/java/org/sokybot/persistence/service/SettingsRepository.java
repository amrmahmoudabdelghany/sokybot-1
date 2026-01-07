package org.sokybot.persistence.service;

import org.sokybot.persistence.entities.Settings;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Settings entity.
 */
public interface SettingsRepository {
    Settings save(Settings entity);
    Settings saveAndFlush(Settings entity);
    List<Settings> saveAll(List<Settings> entities);
    Optional<Settings> findById(String id);
    List<Settings> findAll();
    void deleteById(String id);
    void delete(Settings entity);
    void deleteAll();
    boolean existsById(String id);
    long count();
}

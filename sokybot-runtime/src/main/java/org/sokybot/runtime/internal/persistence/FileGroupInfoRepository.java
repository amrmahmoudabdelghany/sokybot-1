package org.sokybot.runtime.internal.persistence;

import org.sokybot.runtime.internal.domain.GroupInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * File-based implementation of GroupInfoRepository using Java Properties.
 * Stores data in sokybot-data/groups.properties relative to working directory.
 */
public class FileGroupInfoRepository implements GroupInfoRepository {

    private static final Logger log = LoggerFactory.getLogger(FileGroupInfoRepository.class);
    private static final String DATA_DIR = "sokybot-data";
    private static final String FILE_NAME = "groups.properties";

    private final Path dataFile;
    private final Map<Integer, GroupInfo> cache = new LinkedHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public FileGroupInfoRepository() {
        this.dataFile = Paths.get(DATA_DIR, FILE_NAME);
        ensureDataDirectory();
        loadFromFile();
    }

    private void ensureDataDirectory() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            log.error("Failed to create data directory: {}", DATA_DIR, e);
        }
    }

    private synchronized void loadFromFile() {
        if (!Files.exists(dataFile)) {
            log.info("Data file does not exist, starting with empty repository: {}", dataFile);
            return;
        }

        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(dataFile)) {
            props.load(is);

            // Parse properties to find all groups
            Set<Integer> ids = new HashSet<>();
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("group.") && key.endsWith(".name")) {
                    String idStr = key.substring(6, key.indexOf(".name"));
                    try {
                        ids.add(Integer.parseInt(idStr));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid group id in properties: {}", idStr);
                    }
                }
            }

            for (int id : ids) {
                String name = props.getProperty("group." + id + ".name", "");
                String gamePath = props.getProperty("group." + id + ".gamePath", "");
                GroupInfo info = new GroupInfo(id, name, gamePath);
                cache.put(id, info);
                if (id >= nextId.get()) {
                    nextId.set(id + 1);
                }
            }

            log.info("Loaded {} groups from file", cache.size());
        } catch (IOException e) {
            log.error("Failed to load groups from file: {}", dataFile, e);
        }
    }

    private synchronized void saveToFile() {
        Properties props = new Properties();

        for (GroupInfo info : cache.values()) {
            String prefix = "group." + info.getId();
            props.setProperty(prefix + ".name", info.getName());
            props.setProperty(prefix + ".gamePath", info.getGamePath());
        }

        try (OutputStream os = Files.newOutputStream(dataFile)) {
            props.store(os, "Sokybot Groups Data");
            log.debug("Saved {} groups to file", cache.size());
        } catch (IOException e) {
            log.error("Failed to save groups to file: {}", dataFile, e);
        }
    }

    @Override
    public synchronized GroupInfo save(GroupInfo entity) {
        if (entity.getId() == 0) {
            entity.setId(nextId.getAndIncrement());
        }
        cache.put(entity.getId(), entity);
        saveToFile();
        return entity;
    }

    @Override
    public synchronized Optional<GroupInfo> findById(int id) {
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public synchronized List<GroupInfo> findAll() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public synchronized void deleteById(int id) {
        cache.remove(id);
        saveToFile();
    }

    @Override
    public synchronized void delete(GroupInfo entity) {
        deleteById(entity.getId());
    }

    @Override
    public synchronized boolean existsById(int id) {
        return cache.containsKey(id);
    }

    @Override
    public synchronized long count() {
        return cache.size();
    }

    @Override
    public synchronized Optional<GroupInfo> findByName(String name) {
        return cache.values().stream()
                .filter(g -> g.getName().equals(name))
                .findFirst();
    }

    @Override
    public synchronized Optional<GroupInfo> findByGamePath(String gamePath) {
        return cache.values().stream()
                .filter(g -> g.getGamePath().equals(gamePath))
                .findFirst();
    }
}

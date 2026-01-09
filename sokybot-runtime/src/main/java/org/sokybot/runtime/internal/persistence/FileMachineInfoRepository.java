package org.sokybot.runtime.internal.persistence;

import org.sokybot.runtime.internal.domain.MachineInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * File-based implementation of MachineInfoRepository using Java Properties.
 * Stores data in sokybot-data/machines.properties relative to working directory.
 */
public class FileMachineInfoRepository implements MachineInfoRepository {

    private static final Logger log = LoggerFactory.getLogger(FileMachineInfoRepository.class);
    private static final String DATA_DIR = "sokybot-data";
    private static final String FILE_NAME = "machines.properties";

    private final Path dataFile;
    private final Map<Integer, MachineInfo> cache = new LinkedHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public FileMachineInfoRepository() {
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

            // Parse properties to find all machines
            Set<Integer> ids = new HashSet<>();
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("machine.") && key.endsWith(".name")) {
                    String idStr = key.substring(8, key.indexOf(".name"));
                    try {
                        ids.add(Integer.parseInt(idStr));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid machine id in properties: {}", idStr);
                    }
                }
            }

            for (int id : ids) {
                String name = props.getProperty("machine." + id + ".name", "");
                String groupIdStr = props.getProperty("machine." + id + ".groupId", "0");
                int groupId = 0;
                try {
                    groupId = Integer.parseInt(groupIdStr);
                } catch (NumberFormatException e) {
                    log.warn("Invalid groupId for machine {}: {}", id, groupIdStr);
                }
                MachineInfo info = new MachineInfo(id, groupId, name);
                cache.put(id, info);
                if (id >= nextId.get()) {
                    nextId.set(id + 1);
                }
            }

            log.info("Loaded {} machines from file", cache.size());
        } catch (IOException e) {
            log.error("Failed to load machines from file: {}", dataFile, e);
        }
    }

    private synchronized void saveToFile() {
        Properties props = new Properties();

        for (MachineInfo info : cache.values()) {
            String prefix = "machine." + info.getId();
            props.setProperty(prefix + ".name", info.getMachineName());
            props.setProperty(prefix + ".groupId", String.valueOf(info.getGroupId()));
        }

        try (OutputStream os = Files.newOutputStream(dataFile)) {
            props.store(os, "Sokybot Machines Data");
            log.debug("Saved {} machines to file", cache.size());
        } catch (IOException e) {
            log.error("Failed to save machines to file: {}", dataFile, e);
        }
    }

    @Override
    public synchronized MachineInfo save(MachineInfo entity) {
        if (entity.getId() == 0) {
            entity.setId(nextId.getAndIncrement());
        }
        cache.put(entity.getId(), entity);
        saveToFile();
        return entity;
    }

    @Override
    public synchronized Optional<MachineInfo> findById(int id) {
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public synchronized List<MachineInfo> findAll() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public synchronized void deleteById(int id) {
        cache.remove(id);
        saveToFile();
    }

    @Override
    public synchronized void delete(MachineInfo entity) {
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
    public synchronized List<MachineInfo> findByGroupId(int groupId) {
        return cache.values().stream()
                .filter(m -> m.getGroupId() == groupId)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized Optional<MachineInfo> findByMachineName(String machineName) {
        return cache.values().stream()
                .filter(m -> m.getMachineName().equals(machineName))
                .findFirst();
    }
}

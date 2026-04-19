package org.sokybot.scripting.core.internal;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.scripting.api.IScriptParser;
import org.sokybot.scripting.api.ITravelScript;
import org.sokybot.scripting.api.ScriptParseException;

/**
 * Scans {@code data/scripts/travel} for *.script files and hot-reloads periodically.
 */
final class FilesystemScriptRegistry {

    private static final Logger log = LoggerFactory.getLogger(FilesystemScriptRegistry.class);

    private static final long POLL_INTERVAL_SECONDS = 5L;

    private final IScriptParser parser;
    private final Path scriptDirectory;
    private final Map<String, ITravelScript> scripts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "travel-script-scan");
        t.setDaemon(true);
        return t;
    });
    private volatile ScheduledFuture<?> pollHandle;

    FilesystemScriptRegistry(IScriptParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.scriptDirectory = resolveScriptDir();
    }

    static Path resolveScriptDir() {
        String override = System.getProperty("sokybot.scripts.travel.dir");
        if (override != null && !override.isEmpty()) {
            return Paths.get(override).toAbsolutePath().normalize();
        }
        String dataDir = System.getProperty("sokybot.dataDir", "data");
        return Paths.get(dataDir).resolve("scripts").resolve("travel").toAbsolutePath().normalize();
    }

    void start() {
        try {
            Files.createDirectories(scriptDirectory);
        } catch (IOException e) {
            log.warn("Could not create travel script directory {}: {}", scriptDirectory, e.toString());
        }
        reload();
        pollHandle = scheduler.scheduleAtFixedRate(this::safeReload, POLL_INTERVAL_SECONDS, POLL_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
    }

    void stop() {
        if (pollHandle != null) {
            pollHandle.cancel(false);
        }
        scheduler.shutdownNow();
    }

    private void safeReload() {
        try {
            reload();
        } catch (Exception ex) {
            log.debug("Travel script reload failed: {}", ex.toString());
        }
    }

    synchronized void reload() {
        if (!Files.isDirectory(scriptDirectory)) {
            scripts.clear();
            return;
        }
        Map<String, ITravelScript> next = new LinkedHashMap<>();
        List<Path> files = listScriptFiles(scriptDirectory);
        for (Path file : files) {
            String id = scriptIdFromFile(file);
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                ITravelScript ts = parser.parse(id, reader);
                next.put(id, ts);
            } catch (ScriptParseException | IOException ex) {
                log.warn("Failed to parse travel script {}: {}", file, ex.toString());
            }
        }
        scripts.clear();
        scripts.putAll(next);
    }

    private static List<Path> listScriptFiles(Path dir) {
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.script")) {
            for (Path p : ds) {
                if (Files.isRegularFile(p)) {
                    out.add(p);
                }
            }
        } catch (IOException ex) {
            return Collections.emptyList();
        }
        return out.stream().sorted().collect(Collectors.toList());
    }

    private static String scriptIdFromFile(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    Optional<ITravelScript> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(scripts.get(id));
    }

    Collection<ITravelScript> list() {
        return Collections.unmodifiableCollection(new ArrayList<>(scripts.values()));
    }
}

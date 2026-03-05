package org.sokybot.logging.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.logging.api.ILogStreamService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Default implementation of {@link ILogStreamService}.
 *
 * This implementation keeps a bounded in-memory ring buffer of log entries and
 * exposes filtered streams for machine/group/system scopes.
 *
 * NOTE: Mapping of machine/group/feature currently relies on MDC/context
 * information exposed by the underlying logging implementation. Where such
 * metadata is unavailable, entries will default to SYSTEM category.
 */
@Component(service = ILogStreamService.class, immediate = true)
public class LogStreamServiceImpl implements ILogStreamService, LogListener {

    private static final Logger log = LoggerFactory.getLogger(LogStreamServiceImpl.class);

    private static final int MAX_BUFFER_SIZE = 10_000;

    private final Deque<Map<String, Object>> buffer = new ConcurrentLinkedDeque<>();
    private final Sinks.Many<Map<String, Object>> sink =
            Sinks.many().multicast().onBackpressureBuffer(MAX_BUFFER_SIZE);

    private volatile LogReaderService logReaderService;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    @Reference
    public void setLogReaderService(LogReaderService logReaderService) {
        this.logReaderService = logReaderService;
    }

    public void unsetLogReaderService(LogReaderService logReaderService) {
        if (this.logReaderService == logReaderService) {
            this.logReaderService = null;
        }
    }

    @Activate
    public void activate() {
        if (logReaderService != null) {
            logReaderService.addLogListener(this);
            log.info("LogStreamServiceImpl activated and registered as LogListener");
        } else {
            log.warn("LogStreamServiceImpl activated without LogReaderService; logging stream will be empty");
        }
    }

    @Deactivate
    public void deactivate() {
        if (logReaderService != null) {
            try {
                logReaderService.removeLogListener(this);
            } catch (Exception e) {
                log.debug("Error removing LogListener", e);
            }
        }
        sink.tryEmitComplete();
        buffer.clear();
    }

    // ===================== LogListener =====================

    @Override
    public void logged(LogEntry entry) {
        Map<String, Object> record = mapEntry(entry);

        buffer.addFirst(record);
        while (buffer.size() > MAX_BUFFER_SIZE) {
            buffer.removeLast();
        }

        sink.tryEmitNext(record);
    }

    // ===================== ILogStreamService =====================

    @Override
    public Flux<Map<String, Object>> getStream(String machineFullName,
                                               String levelFilter,
                                               String featureFilter,
                                               int maxEntries) {
        Predicate<Map<String, Object>> predicate = record -> {
            if (!matchesLevel(record, levelFilter)) {
                return false;
            }
            // Machine filtering currently relies on metadata.machineFullName;
            // if not present, the record is treated as SYSTEM and included.
            Map<String, Object> metadata = getMetadata(record);
            String category = (String) record.getOrDefault("category", "SYSTEM");

            if ("MACHINE".equals(category)) {
                if (machineFullName != null) {
                    String recMachine = (String) metadata.get("machineFullName");
                    if (recMachine != null && !machineFullName.equals(recMachine)) {
                        return false;
                    }
                }
                if (featureFilter != null && !"ALL".equalsIgnoreCase(featureFilter)) {
                    String feature = (String) metadata.get("feature");
                    if (feature == null || !featureFilter.equalsIgnoreCase(feature)) {
                        return false;
                    }
                }
            }
            return true;
        };

        // Live stream only – UI components should call getRecentEntries(...)
        // for an initial snapshot if needed.
        return sink.asFlux().filter(predicate);
    }

    @Override
    public Flux<Map<String, Object>> getStreamForGroup(String groupName,
                                                       String levelFilter,
                                                       int maxEntries) {
        Predicate<Map<String, Object>> predicate = record -> {
            if (!matchesLevel(record, levelFilter)) {
                return false;
            }
            String category = (String) record.getOrDefault("category", "SYSTEM");
            Map<String, Object> metadata = getMetadata(record);
            if ("GROUP".equals(category)) {
                if (groupName != null) {
                    String recGroup = (String) metadata.get("groupName");
                    return recGroup != null && groupName.equals(recGroup);
                }
            } else if ("SYSTEM".equals(category)) {
                return true;
            }
            return false;
        };
        return sink.asFlux().filter(predicate);
    }

    @Override
    public Flux<Map<String, Object>> getStreamForSystem(String levelFilter, int maxEntries) {
        Predicate<Map<String, Object>> predicate = record -> {
            if (!matchesLevel(record, levelFilter)) {
                return false;
            }
            String category = (String) record.getOrDefault("category", "SYSTEM");
            return "SYSTEM".equals(category);
        };
        return sink.asFlux().filter(predicate);
    }

    @Override
    public List<Map<String, Object>> getRecentEntries(String machineFullName,
                                                      String levelFilter,
                                                      String featureFilter,
                                                      int limit) {
        Predicate<Map<String, Object>> predicate = record ->
                matchesLevel(record, levelFilter); // basic filter for now

        return takeSnapshot(predicate, limit);
    }

    @Override
    public List<Map<String, Object>> getRecentEntriesForGroup(String groupName,
                                                              String levelFilter,
                                                              int limit) {
        Predicate<Map<String, Object>> predicate = record ->
                matchesLevel(record, levelFilter); // basic filter for now

        return takeSnapshot(predicate, limit);
    }

    @Override
    public List<Map<String, Object>> getRecentEntriesForSystem(String levelFilter, int limit) {
        Predicate<Map<String, Object>> predicate = record ->
                matchesLevel(record, levelFilter); // basic filter for now

        return takeSnapshot(predicate, limit);
    }

    // ===================== Helpers =====================

    private Flux<Map<String, Object>> snapshotThenStream(Predicate<Map<String, Object>> predicate,
                                                         int maxEntries) {
        List<Map<String, Object>> snapshot = takeSnapshot(predicate, maxEntries);

        Flux<Map<String, Object>> initial = Flux.fromIterable(snapshot);
        Flux<Map<String, Object>> live = sink.asFlux().filter(predicate);

        return Flux.concat(initial, live);
    }

    private List<Map<String, Object>> takeSnapshot(Predicate<Map<String, Object>> predicate,
                                                   int limit) {
        List<Map<String, Object>> snapshot = new ArrayList<>();
        for (Map<String, Object> record : buffer) {
            if (predicate.test(record)) {
                snapshot.add(record);
                if (snapshot.size() >= limit) {
                    break;
                }
            }
        }
        return snapshot;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMetadata(Map<String, Object> record) {
        Object raw = record.get("metadata");
        if (raw instanceof Map) {
            return (Map<String, Object>) raw;
        }
        Map<String, Object> metadata = new ConcurrentHashMap<>();
        record.put("metadata", metadata);
        return metadata;
    }

    private boolean matchesLevel(Map<String, Object> record, String levelFilter) {
        if (levelFilter == null || "ALL".equalsIgnoreCase(levelFilter)) {
            return true;
        }
        String level = (String) record.get("level");
        return level != null && levelFilter.equalsIgnoreCase(level);
    }

    private Map<String, Object> mapEntry(LogEntry entry) {
        Map<String, Object> record = new ConcurrentHashMap<>();

        record.put("id", UUID.randomUUID().toString());

        long time = entry.getTime();
        record.put("timestamp", TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(time)));

        LogLevel level = entry.getLogLevel();
        record.put("level", level != null ? level.name() : "INFO");

        record.put("source", entry.getLoggerName());
        record.put("thread", safeThreadName(entry));
        record.put("message", Objects.toString(entry.getMessage(), ""));

        Throwable t = entry.getException();
        if (t != null) {
            record.put("stackTrace", stackTraceToString(t));
        }

        // Attempt to capture context / MDC when available (implementation-specific)
        Map<String, Object> metadata = new ConcurrentHashMap<>();
        // Some Pax Logging implementations expose a context map; use reflection defensively.
        try {
            Object contextMap = entry.getClass().getMethod("getContextMap").invoke(entry);
            if (contextMap instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> ctxMap = (Map<Object, Object>) contextMap;
                for (Map.Entry<Object, Object> e : ctxMap.entrySet()) {
                    Object k = e.getKey();
                    Object v = e.getValue();
                    if (k != null && v != null) {
                        metadata.put(String.valueOf(k), String.valueOf(v));
                    }
                }
            }
        } catch (Exception ignored) {
            // No-op; metadata remains empty unless MDC is available.
        }

        // Derive canonical metadata keys and strict category from MDC hints.
        String category = "SYSTEM";
        if (!metadata.isEmpty()) {
            Object explicitCategory = metadata.get("sokybot.log.category");
            if (explicitCategory != null) {
                category = String.valueOf(explicitCategory).toUpperCase(Locale.ROOT);
            }

            String machineFullName = asString(metadata.get("sokybot.log.machineFullName"));
            if (machineFullName != null && !machineFullName.isEmpty()) {
                metadata.put("machineFullName", machineFullName);
            }

            String groupName = asString(metadata.get("sokybot.log.groupName"));
            if (groupName != null && !groupName.isEmpty()) {
                metadata.put("groupName", groupName);
            }

            String feature = asString(metadata.get("sokybot.log.feature"));
            if (feature != null && !feature.isEmpty()) {
                metadata.put("feature", feature);
            }

            // If category was not explicitly set, infer it from available keys.
            if ("SYSTEM".equals(category)) {
                if (machineFullName != null && !machineFullName.isEmpty()) {
                    category = "MACHINE";
                } else if (groupName != null && !groupName.isEmpty()) {
                    category = "GROUP";
                }
            }

            record.put("metadata", metadata);
        }

        record.put("category", category);

        return record;
    }

    private static String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private String safeThreadName(LogEntry entry) {
        try {
            Object threadInfo = entry.getThreadInfo();
            if (threadInfo != null) {
                return threadInfo.toString();
            }
        } catch (NoSuchMethodError | Exception ignored) {
            // Older LogEntry implementations may not have getThreadInfo
        }
        return "unknown";
    }

    private String stackTraceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
        }
        return sw.toString();
    }
}


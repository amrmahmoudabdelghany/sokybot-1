package org.sokybot.webview;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.logging.api.ILogStreamService;
import org.sokybot.webview.api.IWebviewConfigurator;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;

/**
 * Registers the global System Log declarative page backed by ILogStreamService.
 *
 * This page uses the same JSON schema as the machine Log page
 * (scripts/pages/Log.json) and streams SYSTEM-level logs only.
 */
@Component(service = SystemLogPageRegistrar.class, immediate = true)
public class SystemLogPageRegistrar {

    private static final Logger log = LoggerFactory.getLogger(SystemLogPageRegistrar.class);

    private static final String PAGE_ID = "SystemLog";
    private static final String STREAM_ID = "Log";

    private static final int DEFAULT_MAX_ENTRIES = 500;

    private final ObjectMapper mapper = new ObjectMapper();

    private IWebviewConfigurator webviewConfigurator;
    private ILogStreamService logStreamService;

    @Reference
    public void setWebviewConfigurator(IWebviewConfigurator configurator) {
        this.webviewConfigurator = configurator;
    }

    @Reference
    public void setLogStreamService(ILogStreamService service) {
        this.logStreamService = service;
    }

    @Activate
    public void activate() {
        if (webviewConfigurator == null || logStreamService == null) {
            log.warn("SystemLogPageRegistrar activated without required services");
            return;
        }

        Map<String, Object> schema = loadLogSchema();

        webviewConfigurator.addDeclarativePage(
                PAGE_ID,
                "System Log",
                "FileText",
                schema);

        webviewConfigurator.registerActionHandler(PAGE_ID, (action, data) -> {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> logState = new HashMap<>();

            String level = "ALL";
            Object value = data != null ? data.get("value") : null;
            if (value instanceof String) {
                String s = (String) value;
                if (!s.isEmpty()) {
                    level = s;
                }
            }

            logState.put("level", level);
            logState.put("feature", "ALL");
            response.put("Log", logState);
            response.put("success", Boolean.TRUE);

            return response;
        });

        webviewConfigurator.registerSchemaHandler(PAGE_ID, request -> {
            int maxEntries = DEFAULT_MAX_ENTRIES;
            Map<String, Object> state = buildInitialState("ALL", maxEntries);
            Map<String, Object> response = new HashMap<>();
            response.put("schema", schema);
            response.put("state", state);
            return response;
        });

        webviewConfigurator.registerStreamHandler(PAGE_ID, STREAM_ID,
                params -> {
                    String level = stringOrDefault(params.get("level"), "ALL");
                    int maxEntries = intOrDefault(params.get("maxEntries"), DEFAULT_MAX_ENTRIES);

                    Map<String, Object> initialState = buildInitialState(level, maxEntries);

                    return Flux.concat(
                            Flux.just(initialState),
                            logStreamService
                                    .getStreamForSystem(level, maxEntries)
                                    .scan(initialState, (state, entry) -> {
                                        @SuppressWarnings("unchecked")
                                        List<Map<String, Object>> events =
                                                (List<Map<String, Object>>) state.getOrDefault("events",
                                                        Collections.emptyList());
                                        events = new java.util.ArrayList<>(events);
                                        events.add(0, entry);
                                        while (events.size() > maxEntries) {
                                            events.remove(events.size() - 1);
                                        }
                                        Map<String, Object> next = new HashMap<>();
                                        next.put("events", events);
                                        next.put("maxEvents", maxEntries);
                                        // Keep a projection under Log for existing schemas
                                        Map<String, Object> logState = new HashMap<>();
                                        logState.put("events", events);
                                        logState.put("maxEvents", maxEntries);
                                        next.put("Log", logState);
                                        return next;
                                    }));
                },
                STREAM_ID);

        log.info("System Log declarative page registered as {}", PAGE_ID);
    }

    private Map<String, Object> loadLogSchema() {
        // Reuse the existing Log.json schema from scripts/pages.
        String pagesDir = System.getProperty("sokybot.pages.dir", "scripts/pages");
        Path schemaPath = Paths.get(pagesDir).resolve("Log.json");
        if (Files.exists(schemaPath)) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> schema = mapper.readValue(Files.readAllBytes(schemaPath), Map.class);
                return schema;
            } catch (IOException e) {
                log.error("Failed to load Log.json schema for SystemLog page: {}", e.getMessage());
            }
        }
        log.warn("Log.json schema not found at {}, using empty schema for SystemLog", schemaPath);
        return new HashMap<>();
    }

    private Map<String, Object> buildInitialState(String level, int maxEntries) {
        List<Map<String, Object>> entries =
                logStreamService.getRecentEntriesForSystem(level, maxEntries);

        Map<String, Object> state = new HashMap<>();
        state.put("events", entries);
        state.put("maxEvents", maxEntries);

        Map<String, Object> logState = new HashMap<>();
        logState.put("events", entries);
        logState.put("maxEvents", maxEntries);
        logState.put("level", level);
        logState.put("feature", "ALL");
        state.put("Log", logState);

        return state;
    }

    private static String stringOrDefault(Object value, String def) {
        if (value instanceof String) {
            String s = (String) value;
            if (!s.isEmpty()) {
                return s;
            }
        }
        return def;
    }

    private static int intOrDefault(Object value, int def) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return def;
    }
}


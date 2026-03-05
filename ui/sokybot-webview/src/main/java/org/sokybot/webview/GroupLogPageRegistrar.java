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
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.webview.api.IWebviewConfigurator;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;

/**
 * Registers group-level log pages backed by ILogStreamService.
 *
 * Each group gets a declarative page with pageId = "GroupLog_<groupName>" and
 * reuses the Log.json schema for rendering.
 */
@Component(service = GroupLogPageRegistrar.class, immediate = true)
public class GroupLogPageRegistrar {

    private static final Logger log = LoggerFactory.getLogger(GroupLogPageRegistrar.class);

    private static final String STREAM_ID = "Log";
    private static final int DEFAULT_MAX_ENTRIES = 500;

    private final ObjectMapper mapper = new ObjectMapper();

    private IWebviewConfigurator webviewConfigurator;
    private ILogStreamService logStreamService;
    private ISokybotContext sokybotContext;

    @Reference
    public void setWebviewConfigurator(IWebviewConfigurator configurator) {
        this.webviewConfigurator = configurator;
    }

    @Reference
    public void setLogStreamService(ILogStreamService service) {
        this.logStreamService = service;
    }

    @Reference
    public void setSokybotContext(ISokybotContext ctx) {
        this.sokybotContext = ctx;
    }

    @Activate
    public void activate() {
        if (webviewConfigurator == null || logStreamService == null || sokybotContext == null) {
            log.warn("GroupLogPageRegistrar activated without required services (webview/logging/runtime)");
            return;
        }

        Map<String, Object> schema = loadLogSchema();

        IGroupContext[] groups = sokybotContext.getGroups();
        if (groups == null || groups.length == 0) {
            log.info("No groups found for Group Log pages");
            return;
        }

        for (IGroupContext group : groups) {
            String groupName = group.name();
            if (groupName == null || groupName.isEmpty()) {
                continue;
            }
            registerGroupLogPage(groupName, schema);
        }
    }

    private void registerGroupLogPage(String groupName, Map<String, Object> schema) {
        String pageId = "GroupLog_" + groupName;

        webviewConfigurator.addDeclarativePage(
                pageId,
                "Log",
                "FileText",
                schema);

        webviewConfigurator.registerActionHandler(pageId, (action, data) -> {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> logState = new HashMap<>();

            String level = "ALL";
            String feature = "ALL";
            Object value = data != null ? data.get("value") : null;
            if (value instanceof String) {
                String s = (String) value;
                if (!s.isEmpty()) {
                    if ("setLogLevel".equals(action)) {
                        level = s;
                    } else if ("setLogFeature".equals(action)) {
                        feature = s;
                    }
                }
            }

            logState.put("level", level);
            logState.put("feature", feature);
            response.put("Log", logState);
            response.put("success", Boolean.TRUE);

            return response;
        });

        webviewConfigurator.registerSchemaHandler(pageId, request -> {
            int maxEntries = DEFAULT_MAX_ENTRIES;
            String level = "ALL";
            Map<String, Object> state = buildInitialState(groupName, level, maxEntries);
            Map<String, Object> response = new HashMap<>();
            response.put("schema", schema);
            response.put("state", state);
            return response;
        });

        webviewConfigurator.registerStreamHandler(pageId, STREAM_ID,
                params -> {
                    String level = stringOrDefault(params.get("level"), "ALL");
                    int maxEntries = intOrDefault(params.get("maxEntries"), DEFAULT_MAX_ENTRIES);

                    Map<String, Object> initialState = buildInitialState(groupName, level, maxEntries);

                    return Flux.concat(
                            Flux.just(initialState),
                            logStreamService
                                    .getStreamForGroup(groupName, level, maxEntries)
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
                                        Map<String, Object> logState = new HashMap<>();
                                        logState.put("events", events);
                                        logState.put("maxEvents", maxEntries);
                                        next.put("Log", logState);
                                        return next;
                                    }));
                },
                STREAM_ID);

        log.info("Group Log declarative page registered for group {} as {}", groupName, pageId);
    }

    private Map<String, Object> loadLogSchema() {
        String pagesDir = System.getProperty("sokybot.pages.dir", "scripts/pages");
        Path schemaPath = Paths.get(pagesDir).resolve("Log.json");
        if (Files.exists(schemaPath)) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> schema = mapper.readValue(Files.readAllBytes(schemaPath), Map.class);
                return schema;
            } catch (IOException e) {
                log.error("Failed to load Log.json schema for GroupLog pages: {}", e.getMessage());
            }
        }
        log.warn("Log.json schema not found for GroupLog pages; using empty schema");
        return new HashMap<>();
    }

    private Map<String, Object> buildInitialState(String groupName, String level, int maxEntries) {
        List<Map<String, Object>> entries =
                logStreamService.getRecentEntriesForGroup(groupName, level, maxEntries);

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


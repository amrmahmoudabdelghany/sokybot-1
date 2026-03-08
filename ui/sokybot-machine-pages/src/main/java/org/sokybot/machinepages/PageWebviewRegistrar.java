package org.sokybot.machinepages;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.sokybot.logging.api.ILogStreamService;
import org.sokybot.machinepages.api.IScriptedPage;
import org.sokybot.packetsniffer.api.IPacketSnifferPage;
import org.sokybot.packetsniffer.api.IPacketSnifferRegistry;
import org.sokybot.webview.api.IWebviewConfigurator;

/**
 * Handles registration of scripted pages with the webview system:
 * schema handlers, action handlers, and stream handlers.
 *
 * Separated from {@link MachinePagesActivator} to keep each class
 * focused on a single responsibility.
 */
@Component(service = PageWebviewRegistrar.class, immediate = true)
public class PageWebviewRegistrar {

    private static final String LOG_STREAM_ID = "Log";

    private IWebviewConfigurator webviewConfigurator;
    private ILogStreamService logStreamService;
    private IPacketSnifferRegistry packetSnifferRegistry;
    private ScriptPageLoader pageLoader;

    @Reference
    public void setWebviewConfigurator(IWebviewConfigurator webviewConfigurator) {
        this.webviewConfigurator = webviewConfigurator;
    }

    @Reference
    public void setPageLoader(ScriptPageLoader pageLoader) {
        this.pageLoader = pageLoader;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setLogStreamService(ILogStreamService logStreamService) {
        this.logStreamService = logStreamService;
    }

    public void unsetLogStreamService(ILogStreamService logStreamService) {
        if (this.logStreamService == logStreamService) {
            this.logStreamService = null;
        }
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setPacketSnifferRegistry(IPacketSnifferRegistry packetSnifferRegistry) {
        this.packetSnifferRegistry = packetSnifferRegistry;
    }

    public void unsetPacketSnifferRegistry(IPacketSnifferRegistry packetSnifferRegistry) {
        if (this.packetSnifferRegistry == packetSnifferRegistry) {
            this.packetSnifferRegistry = null;
        }
    }

    /**
     * Register a page with the webview configurator: add the declarative page,
     * schema handler, action handler, and stream handlers.
     */
    public void registerPage(String pageId, String pageName, String machineFullName, IScriptedPage page) {
        if (webviewConfigurator == null) return;

        Map<String, Object> schema = pageLoader.loadSchema(pageName);
        if (schema.isEmpty()) {
            schema = page.getSchema();
        }

        webviewConfigurator.addDeclarativePage(pageId, page.getTitle(), page.getIcon(), schema);

        registerSchemaHandler(pageId, pageName, machineFullName, page);
        webviewConfigurator.registerActionHandler(pageId, page::handleAction);
        registerStreamHandler(pageId, pageName, machineFullName, page);
        registerSnifferStreams(pageId, pageName, machineFullName);
    }

    /**
     * Remove a page from the webview.
     */
    public void unregisterPage(String pageId) {
        if (webviewConfigurator != null) {
            webviewConfigurator.removePage(pageId);
        }
    }

    // ---- Private helpers ----

    private void registerSchemaHandler(String pageId, String pageName, String machineFullName, IScriptedPage page) {
        if ("log".equalsIgnoreCase(pageName) && logStreamService != null) {
            webviewConfigurator.registerSchemaHandler(pageId, request -> {
                Map<String, Object> result = new HashMap<>();
                result.put("schema", pageLoader.loadSchema(pageName));

                String level = "ALL";
                String feature = "ALL";
                int maxEntries = 500;

                var entries = logStreamService.getRecentEntries(machineFullName, level, feature, maxEntries);
                result.put("state", buildLogState(entries, maxEntries, level, feature));
                return result;
            });
        } else {
            webviewConfigurator.registerSchemaHandler(pageId, request -> {
                Map<String, Object> result = new HashMap<>();
                result.put("schema", pageLoader.loadSchema(pageName));
                result.put("state", page.getInitialState());
                return result;
            });
        }
    }

    private void registerStreamHandler(String pageId, String pageName, String machineFullName, IScriptedPage page) {
        if ("log".equalsIgnoreCase(pageName) && logStreamService != null) {
            webviewConfigurator.registerStreamHandler(pageId, LOG_STREAM_ID,
                    params -> {
                        String level = stringOrDefault(params.get("level"), "ALL");
                        String feature = stringOrDefault(params.get("feature"), "ALL");
                        int maxEntries = intOrDefault(params.get("maxEntries"), 500);

                        var initial = logStreamService.getRecentEntries(machineFullName, level, feature, maxEntries);
                        var initialState = buildLogStreamState(initial, maxEntries, level, feature);

                        return reactor.core.publisher.Flux
                                .concat(
                                        reactor.core.publisher.Flux.just(initialState),
                                        logStreamService
                                                .getStream(machineFullName, level, feature, maxEntries)
                                                .scan(initialState, (state, entry) -> {
                                                    @SuppressWarnings("unchecked")
                                                    java.util.List<java.util.Map<String, Object>> events =
                                                            (java.util.List<java.util.Map<String, Object>>) state
                                                                    .getOrDefault("events",
                                                                            java.util.Collections.emptyList());
                                                    events = new java.util.ArrayList<>(events);
                                                    events.add(0, entry);
                                                    while (events.size() > maxEntries) {
                                                        events.remove(events.size() - 1);
                                                    }
                                                    return buildLogStreamState(events, maxEntries, level, feature);
                                                }));
                    },
                    LOG_STREAM_ID);
        } else {
            webviewConfigurator.registerStreamHandler(pageId, pageName,
                    params -> page.streamData(pageName, params),
                    pageName);
        }
    }

    private void registerSnifferStreams(String pageId, String pageName, String machineFullName) {
        if (packetSnifferRegistry == null) return;

        if ("PacketSniffer".equalsIgnoreCase(pageName)) {
            IPacketSnifferPage sniffer = packetSnifferRegistry.getSniffer(machineFullName);
            if (sniffer != null) {
                webviewConfigurator.registerStreamHandler(pageId, "packets", sniffer::streamPackets, "trafficPackets");
                webviewConfigurator.registerStreamHandler(pageId, "statistics", sniffer::streamStatistics, "statistics");
                sniffer.addStateChangeListener(
                        state -> webviewConfigurator.sendEvent(pageId + ".stateChanged", state));
            }
        } else if ("PacketAnalyzer".equalsIgnoreCase(pageName)) {
            IPacketSnifferPage sniffer = packetSnifferRegistry.getSniffer(machineFullName);
            if (sniffer != null) {
                sniffer.addStateChangeListener(
                        state -> webviewConfigurator.sendEvent(pageId + ".stateChanged", state));
            }
        }
    }

    // ---- State building helpers ----

    private static Map<String, Object> buildLogStreamState(java.util.List<java.util.Map<String, Object>> events,
                                                           int maxEntries, String level, String feature) {
        Map<String, Object> state = new HashMap<>();
        state.put("events", events);
        state.put("maxEvents", maxEntries);
        state.put("level", level);
        state.put("feature", feature);
        return state;
    }

    private static Map<String, Object> buildLogState(java.util.List<java.util.Map<String, Object>> events,
                                                     int maxEntries, String level, String feature) {
        Map<String, Object> state = new HashMap<>();
        state.put("events", events);
        state.put("maxEvents", maxEntries);

        Map<String, Object> logState = new HashMap<>();
        logState.put("events", events);
        logState.put("maxEvents", maxEntries);
        logState.put("level", level);
        logState.put("feature", feature);
        state.put("Log", logState);

        return state;
    }

    private static String stringOrDefault(Object value, String def) {
        if (value instanceof String) {
            String s = (String) value;
            if (!s.isEmpty() && !s.contains("${")) return s;
        }
        return def;
    }

    private static int intOrDefault(Object value, int def) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value != null) {
            String s = value.toString();
            if (s.contains("${")) return def;
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return def;
    }
}
